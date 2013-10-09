/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.descriptors.serialization;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorImpl;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.lazy.LazyResolveTestUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.storage.LockBasedStorageManager;
import org.jetbrains.jet.test.util.RecursiveDescriptorComparator;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.jetbrains.jet.descriptors.serialization.ClassSerializationUtil.getClassId;
import static org.jetbrains.jet.descriptors.serialization.NameSerializationUtil.createNameResolver;
import static org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer.UNSUPPORTED;
import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.IGNORE_KOTLIN_SOURCES;
import static org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils.naiveKotlinFqName;

public abstract class AbstractDescriptorSerializationTest extends KotlinTestWithEnvironment {
    public static final Name TEST_PACKAGE_NAME = Name.identifier("test");

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    protected void doTest(@NotNull String path) throws IOException {
        File ktFile = new File(path);
        ModuleDescriptor moduleDescriptor = LazyResolveTestUtil.resolveEagerly(Collections.singletonList(
                JetTestUtils.createFile(ktFile.getName(), FileUtil.loadFile(ktFile), getProject())
        ), getEnvironment());

        NamespaceDescriptor testNamespace = moduleDescriptor.getNamespace(FqName.topLevel(TEST_PACKAGE_NAME));
        assert testNamespace != null;

        InjectorForJavaDescriptorResolver injector = new InjectorForJavaDescriptorResolver(getProject(), new BindingTraceContext());
        JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();

        NamespaceDescriptor deserialized = serializeAndDeserialize(javaDescriptorResolver, testNamespace);

        RecursiveDescriptorComparator
                .validateAndCompareDescriptors(testNamespace, deserialized, RecursiveDescriptorComparator.RECURSIVE, null);
    }

    @NotNull
    private static NamespaceDescriptor serializeAndDeserialize(
            @NotNull JavaDescriptorResolver javaDescriptorResolver,
            @NotNull NamespaceDescriptor testPackage
    ) {
        List<ClassDescriptor> classesAndObjects = getAllClassesAndObjects(testPackage.getMemberScope());

        Map<ClassDescriptor, byte[]> serializedClasses = serializeClasses(classesAndObjects);
        byte[] serializedPackage = serializePackage(testPackage);

        Map<String, ClassData> classDataMap = new HashMap<String, ClassData>();

        for (Map.Entry<ClassDescriptor, byte[]> entry : serializedClasses.entrySet()) {
            String key = naiveKotlinFqName(entry.getKey()).asString();
            ClassData value = ClassData.read(entry.getValue(), JavaProtoBufUtil.getExtensionRegistry());
            classDataMap.put(key, value);
        }

        NamespaceDescriptorImpl namespace = JetTestUtils.createTestNamespace(TEST_PACKAGE_NAME);

        DescriptorFinder descriptorFinder = new DescriptorFinderFromClassDataOrJava(javaDescriptorResolver, namespace, classDataMap);

        for (ClassDescriptor classDescriptor : classesAndObjects) {
            ClassId classId = getClassId(classDescriptor);
            ClassDescriptor descriptor = descriptorFinder.findClass(classId);
            assert descriptor != null : "Class not loaded: " + classId;
            namespace.getMemberScope().addClassifierDescriptor(descriptor);
        }

        PackageData data = PackageData.read(serializedPackage, JavaProtoBufUtil.getExtensionRegistry());

        DescriptorDeserializer deserializer = DescriptorDeserializer
                .create(new LockBasedStorageManager(), namespace, data.getNameResolver(), descriptorFinder, UNSUPPORTED);
        for (ProtoBuf.Callable proto : data.getPackageProto().getMemberList()) {
            CallableMemberDescriptor descriptor = deserializer.loadCallable(proto);
            if (descriptor instanceof FunctionDescriptor) {
                namespace.getMemberScope().addFunctionDescriptor((FunctionDescriptor) descriptor);
            }
            else if (descriptor instanceof PropertyDescriptor) {
                namespace.getMemberScope().addPropertyDescriptor((PropertyDescriptor) descriptor);
            }
            else {
                throw new IllegalStateException("Unknown descriptor type: " + descriptor);
            }
        }
        namespace.getMemberScope().changeLockLevel(WritableScope.LockLevel.READING);

        return namespace;
    }

    @NotNull
    private static List<ClassDescriptor> getAllClassesAndObjects(@NotNull JetScope scope) {
        List<ClassDescriptor> classes = new ArrayList<ClassDescriptor>();
        for (DeclarationDescriptor descriptor : scope.getAllDescriptors()) {
            if (descriptor instanceof ClassDescriptor) {
                classes.add((ClassDescriptor) descriptor);
            }
        }
        return classes;
    }

    @NotNull
    private static byte[] serializePackage(@NotNull NamespaceDescriptor descriptor) {
        DescriptorSerializer serializer = new DescriptorSerializer();
        ProtoBuf.Package proto = serializer.packageProto(descriptor).build();
        PackageData data = new PackageData(createNameResolver(serializer.getNameTable()), proto);
        return data.toBytes();
    }

    @NotNull
    private static Map<ClassDescriptor, byte[]> serializeClasses(@NotNull Collection<ClassDescriptor> classes) {
        final Map<ClassDescriptor, byte[]> serializedClasses = new HashMap<ClassDescriptor, byte[]>();
        final DescriptorSerializer serializer = new DescriptorSerializer();

        ClassSerializationUtil.serializeClasses(classes, serializer, new ClassSerializationUtil.Sink() {
            @Override
            public void writeClass(@NotNull ClassDescriptor classDescriptor, @NotNull ProtoBuf.Class classProto) {
                ClassData data = new ClassData(createNameResolver(serializer.getNameTable()), classProto);
                serializedClasses.put(classDescriptor, data.toBytes());
            }
        });

        return serializedClasses;
    }

    private static class DescriptorFinderFromClassDataOrJava extends AbstractDescriptorFinder {
        private final JavaDescriptorResolver javaDescriptorResolver;
        private final NamespaceDescriptor packageForClasses;
        private final Map<String, ClassData> classDataMap;

        public DescriptorFinderFromClassDataOrJava(
                @NotNull JavaDescriptorResolver javaDescriptorResolver,
                @NotNull NamespaceDescriptor packageForClasses,
                @NotNull Map<String, ClassData> classDataMap
        ) {
            super(new LockBasedStorageManager(), UNSUPPORTED);
            this.javaDescriptorResolver = javaDescriptorResolver;
            this.packageForClasses = packageForClasses;
            this.classDataMap = classDataMap;
        }

        @Nullable
        @Override
        public ClassDescriptor findClass(@NotNull ClassId classId) {
            ClassDescriptor found = super.findClass(classId);
            return found != null ? found : javaDescriptorResolver.resolveClass(classId.asSingleFqName().toSafe(), IGNORE_KOTLIN_SOURCES);
        }

        @Nullable
        @Override
        protected ClassData getClassData(@NotNull ClassId classId) {
            return classDataMap.get(classId.asSingleFqName().asString());
        }

        @Nullable
        @Override
        public NamespaceDescriptor findPackage(@NotNull FqName name) {
            assert DescriptorUtils.getFQName(packageForClasses).equals(name.toUnsafe()) : name + " : " + packageForClasses;
            return packageForClasses;
        }

        @NotNull
        @Override
        public Collection<Name> getClassNames(@NotNull FqName packageName) {
            throw new UnsupportedOperationException();
        }
    }
}
