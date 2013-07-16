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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.MessageLite;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.NullableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorImpl;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.lazy.LazyResolveTestUtil;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNullable;
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNullableImpl;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.test.util.NamespaceComparator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.jetbrains.jet.descriptors.serialization.ClassSerializationUtil.getClassId;
import static org.jetbrains.jet.descriptors.serialization.NameSerializationUtil.*;
import static org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer.UNSUPPORTED;
import static org.jetbrains.jet.lang.resolve.java.resolver.DeserializedResolverUtils.naiveKotlinFqName;

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

        JavaDescriptorResolver javaDescriptorResolver = new InjectorForJavaDescriptorResolver(
                getProject(), new BindingTraceContext(), moduleDescriptor).getJavaDescriptorResolver();

        Collection<DeclarationDescriptor> initial = new ArrayList<DeclarationDescriptor>();
        initial.addAll(testNamespace.getMemberScope().getAllDescriptors());
        initial.addAll(testNamespace.getMemberScope().getObjectDescriptors());

        NamespaceDescriptor deserialized = serializeAndDeserialize(javaDescriptorResolver, initial);

        NamespaceComparator.validateAndCompareNamespaces(testNamespace, deserialized, NamespaceComparator.RECURSIVE, null);
    }

    @NotNull
    private static NamespaceDescriptor serializeAndDeserialize(
            @NotNull JavaDescriptorResolver javaDescriptorResolver,
            @NotNull Collection<DeclarationDescriptor> initial
    ) throws IOException {
        List<ClassDescriptor> classes = Lists.newArrayList();
        List<CallableMemberDescriptor> callables = Lists.newArrayList();
        for (DeclarationDescriptor descriptor : initial) {
            if (descriptor instanceof ClassDescriptor) {
                classes.add((ClassDescriptor) descriptor);
            }
            else if (descriptor instanceof CallableMemberDescriptor) {
                callables.add((CallableMemberDescriptor) descriptor);
            }
            else {
                fail("Unsupported descriptor type: " + descriptor);
            }
        }
        return getDeserializedDescriptors(javaDescriptorResolver, classes, callables);
    }

    @NotNull
    private static NamespaceDescriptor getDeserializedDescriptors(
            @NotNull JavaDescriptorResolver javaDescriptorResolver,
            @NotNull List<ClassDescriptor> classes,
            @NotNull List<CallableMemberDescriptor> callables
    ) throws IOException {
        Map<ClassDescriptor, byte[]> serializedClasses = serializeClasses(classes);
        byte[] serializedCallables = serializeCallables(callables);

        final Map<String, ClassData> classDataMap = Maps.newHashMap();

        for (Map.Entry<ClassDescriptor, byte[]> entry : serializedClasses.entrySet()) {
            String key = naiveKotlinFqName(entry.getKey()).asString();
            ClassData value = ClassData.read(entry.getValue(), JavaProtoBufUtil.getExtensionRegistry());
            classDataMap.put(key, value);
        }

        NamespaceDescriptorImpl namespace = JetTestUtils.createTestNamespace(TEST_PACKAGE_NAME);
        DescriptorFinder javaDescriptorFinder = new JavaDescriptorFinder(javaDescriptorResolver);

        DescriptorFinderImpl descriptorFinder = new DescriptorFinderImpl(
                javaDescriptorFinder, namespace,
                new NullableFunction<String, ClassData>() {
                    @Nullable
                    @Override
                    public ClassData fun(String fqName) {
                        return classDataMap.get(fqName);
                    }
                }
        );

        for (ClassDescriptor classDescriptor : classes) {
            ClassId classId = getClassId(classDescriptor);
            ClassDescriptor descriptor = descriptorFinder.findClass(classId);
            assert descriptor != null : "Class not loaded: " + classId;
            if (descriptor.getKind().isObject()) {
                namespace.getMemberScope().addObjectDescriptor(descriptor);
            }
            else {
                namespace.getMemberScope().addClassifierDescriptor(descriptor);
            }
        }

        ByteArrayInputStream in = new ByteArrayInputStream(serializedCallables);
        NameResolver nameResolver = deserializeNameResolver(in);

        List<ProtoBuf.Callable> callableProtos = Lists.newArrayList();
        for (int i = 0; i < callables.size(); i++) {
            ProtoBuf.Callable proto = ProtoBuf.Callable.parseDelimitedFrom(in);
            callableProtos.add(proto);
        }

        DescriptorDeserializer deserializer =
                DescriptorDeserializer.create(new LockBasedStorageManager(), namespace, nameResolver, descriptorFinder, UNSUPPORTED);
        for (ProtoBuf.Callable proto : callableProtos) {
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
    private static byte[] serializeCallables(@NotNull List<CallableMemberDescriptor> callables) throws IOException {
        DescriptorSerializer serializer = new DescriptorSerializer();
        List<MessageLite> messages = Lists.newArrayList();
        for (CallableMemberDescriptor callable : callables) {
            messages.add(serializer.callableProto(callable).build());
        }

        ByteArrayOutputStream serializedCallables = new ByteArrayOutputStream();
        serializeNameTable(serializedCallables, serializer.getNameTable());

        for (MessageLite message : messages) {
            message.writeDelimitedTo(serializedCallables);
        }

        return serializedCallables.toByteArray();
    }

    @NotNull
    private static Map<ClassDescriptor, byte[]> serializeClasses(@NotNull Collection<ClassDescriptor> classes) {
        final Map<ClassDescriptor, byte[]> serializedClasses = Maps.newHashMap();
        final DescriptorSerializer serializer = new DescriptorSerializer();

        ClassSerializationUtil.serializeClasses(classes, serializer, new ClassSerializationUtil.Sink() {
            @Override
            public void writeClass(@NotNull ClassDescriptor classDescriptor, @NotNull ProtoBuf.Class classProto) {
                serializedClasses.put(classDescriptor, new ClassData(createNameResolver(serializer.getNameTable()), classProto).toBytes());
            }
        });

        return serializedClasses;
    }

    private static class DescriptorFinderImpl implements DescriptorFinder {
        private final DescriptorFinder parentResolver;

        private final DeclarationDescriptor parentForClasses;
        private final NullableFunction<String, ClassData> classData;

        private final MemoizedFunctionToNullable<ClassId, ClassDescriptor> classes;

        public DescriptorFinderImpl(
                @NotNull DescriptorFinder parentResolver,
                @NotNull DeclarationDescriptor parentForClasses,
                @NotNull NullableFunction<String, ClassData> classData
        ) {
            this.parentResolver = parentResolver;

            this.parentForClasses = parentForClasses;

            this.classData = classData;

            this.classes = new MemoizedFunctionToNullableImpl<ClassId, ClassDescriptor>() {
                @Nullable
                @Override
                protected ClassDescriptor doCompute(@NotNull ClassId classId) {
                    DeclarationDescriptor containingDeclaration =
                            classId.isTopLevelClass() ? DescriptorFinderImpl.this.parentForClasses : findClass(classId.getOuterClassId());
                    assert containingDeclaration != null : "Containing declaration not found: " + classId.getOuterClassId();
                    return resolveClass(containingDeclaration, classId);
                }
            };
        }

        @Nullable
        private ClassDescriptor resolveClass(@NotNull DeclarationDescriptor containingDeclaration, @NotNull ClassId classId) {
            ClassData classData = this.classData.fun(classId.asSingleFqName().asString());
            if (classData == null) {
                return parentResolver.findClass(classId);
            }

            return new DeserializedClassDescriptor(classId, new LockBasedStorageManager(), containingDeclaration,
                    classData.getNameResolver(), UNSUPPORTED, this, classData.getClassProto(), null);
        }

        @Nullable
        @Override
        public ClassDescriptor findClass(@NotNull ClassId classId) {
            return classes.fun(classId);
        }

        @Nullable
        @Override
        public NamespaceDescriptor findPackage(@NotNull FqName name) {
            return null;
        }

        @NotNull
        @Override
        public Collection<Name> getClassNames(@NotNull FqName packageName) {
            throw new UnsupportedOperationException();
        }
    }

    private static class JavaDescriptorFinder implements DescriptorFinder {
        private final JavaDescriptorResolver javaDescriptorResolver;

        public JavaDescriptorFinder(JavaDescriptorResolver javaDescriptorResolver) {
            this.javaDescriptorResolver = javaDescriptorResolver;
        }

        @Nullable
        @Override
        public ClassDescriptor findClass(@NotNull ClassId classId) {
            FqNameUnsafe fqNameUnsafe = classId.asSingleFqName();
            assert fqNameUnsafe.isSafe() : "Unsafe fqName made it to Java resolve: " + fqNameUnsafe;
            ClassDescriptor javaClassDescriptor = javaDescriptorResolver.resolveClass(fqNameUnsafe.toSafe());
            if (javaClassDescriptor != null) {
                return javaClassDescriptor;
            }
            NamespaceDescriptor packageDescriptor = findPackage(classId.getPackageFqName());
            if (packageDescriptor == null) {
                throw new IllegalStateException("Java package not found: " + classId.getPackageFqName() + " for " + classId);
            }

            JetScope scope = packageDescriptor.getMemberScope();
            for (Iterator<Name> iterator = classId.getRelativeClassName().pathSegments().iterator(); iterator.hasNext(); ) {
                Name name = iterator.next();
                ClassifierDescriptor classifier = scope.getClassifier(name);
                if (classifier == null) {
                    throw new IllegalStateException("Class not found: " + classId);
                }
                ClassDescriptor classDescriptor = (ClassDescriptor) classifier;
                if (!iterator.hasNext()) {
                    return classDescriptor;
                }
                scope = classDescriptor.getUnsubstitutedInnerClassesScope();
            }

            throw new IllegalStateException("Class not found: " + classId);
        }

        @Nullable
        @Override
        public NamespaceDescriptor findPackage(@NotNull FqName name) {
            return javaDescriptorResolver.resolveNamespace(name);
        }

        @NotNull
        @Override
        public Collection<Name> getClassNames(@NotNull FqName packageName) {
            throw new UnsupportedOperationException();
        }
    }
}
