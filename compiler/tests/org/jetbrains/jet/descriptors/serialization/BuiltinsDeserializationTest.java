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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer;
import org.jetbrains.jet.jvm.compiler.ExpectedLoadErrorsUtil;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.MutablePackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.renderer.DescriptorRendererBuilder;
import org.jetbrains.jet.storage.LockBasedStorageManager;
import org.jetbrains.jet.test.util.RecursiveDescriptorComparator;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BuiltinsDeserializationTest extends KotlinTestWithEnvironment {
    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithJdk(ConfigurationKind.JDK_AND_ANNOTATIONS, TestJdkKind.FULL_JDK);
    }

    public void testBuiltIns() throws Exception {
        Collection<DeclarationDescriptor> allDescriptors = KotlinBuiltIns.getInstance().getBuiltInsPackageScope().getAllDescriptors();
        PackageFragmentDescriptor actualPackage = getDeserializedDescriptorsAsPackage(allDescriptors);

        RecursiveDescriptorComparator.Configuration configuration = RecursiveDescriptorComparator.RECURSIVE.withRenderer(
                new DescriptorRendererBuilder()
                        .setWithDefinedIn(false)
                        .setExcludedAnnotationClasses(Arrays.asList(new FqName(ExpectedLoadErrorsUtil.ANNOTATION_CLASS_NAME)))
                        .setOverrideRenderingPolicy(DescriptorRenderer.OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE)
                        .setVerbose(true)
                        .setAlwaysRenderAny(true)
                        .setPrettyFunctionTypes(false)
                        .build()
        );
        RecursiveDescriptorComparator
                .validateAndCompareDescriptors(KotlinBuiltIns.getInstance().getBuiltInsPackageFragment(), actualPackage, configuration, null);
    }

    private static PackageFragmentDescriptor getDeserializedDescriptorsAsPackage(Collection<DeclarationDescriptor> allDescriptors) {
        DescriptorSerializer serializer = new DescriptorSerializer();

        final Map<ClassId, ProtoBuf.Class> classProtos = serializeClasses(serializer, allDescriptors);

        List<ProtoBuf.Callable> callableProtos = serializeCallables(serializer, allDescriptors);

        final MutablePackageFragmentDescriptor actualPackage = JetTestUtils.createTestPackageFragment(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME);

        final NameResolver nameResolver = NameSerializationUtil.createNameResolver(serializer.getNameTable());

        DescriptorFinder finder = new AbstractDescriptorFinder(new LockBasedStorageManager(), AnnotationDeserializer.UNSUPPORTED) {
            @NotNull
            @Override
            public PackageFragmentDescriptor findPackage(@NotNull FqName fqName) {
                assert fqName.equals(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME) : "Unsupported package: " + fqName;
                return actualPackage;
            }

            @NotNull
            @Override
            public Collection<Name> getClassNames(@NotNull FqName packageName) {
                throw new UnsupportedOperationException();
            }

            @Nullable
            @Override
            protected ClassData getClassData(@NotNull ClassId classId) {
                ProtoBuf.Class classProto = classProtos.get(classId);
                assert classProto != null : "Class not found: " + classId;
                return new ClassData(nameResolver, classProto);
            }

            @Override
            protected void classDescriptorCreated(@NotNull ClassDescriptor classDescriptor) {
                if (!DescriptorUtils.isTopLevelDeclaration(classDescriptor)) return;
                switch (classDescriptor.getKind()) {
                    case CLASS:
                    case TRAIT:
                    case ENUM_CLASS:
                    case ANNOTATION_CLASS:
                    case OBJECT:
                        actualPackage.getMemberScope().addClassifierDescriptor(classDescriptor);
                        break;
                    case ENUM_ENTRY:
                        assert false : "Enum entry appears to be a top-level declaration: " + classDescriptor;
                    case CLASS_OBJECT:
                        assert false : "Class object appears to be a top-level declaration: " + classDescriptor;
                }
            }
        };

        // Make the lazy loader create classes
        for (ClassId classId : classProtos.keySet()) {
            finder.findClass(classId);
        }

        deserializeCallables(callableProtos, actualPackage, nameResolver, finder);

        actualPackage.getMemberScope().changeLockLevel(WritableScope.LockLevel.READING);
        return actualPackage;
    }

    private static Map<ClassId, ProtoBuf.Class> serializeClasses(
            DescriptorSerializer serializer,
            Collection<DeclarationDescriptor> allDescriptors
    ) {
        final Map<ClassId, ProtoBuf.Class> classProtos = Maps.newHashMap();
        ClassSerializationUtil.serializeClasses(allDescriptors, serializer, new ClassSerializationUtil.Sink() {
            @Override
            public void writeClass(
                    @NotNull ClassDescriptor classDescriptor, @NotNull ProtoBuf.Class classProto
            ) {
                classProtos.put(ClassSerializationUtil.getClassId(classDescriptor), classProto);
            }
        });
        return classProtos;
    }

    private static List<ProtoBuf.Callable> serializeCallables(
            DescriptorSerializer serializer,
            Collection<DeclarationDescriptor> allDescriptors
    ) {
        List<ProtoBuf.Callable> callableProtos = Lists.newArrayList();
        for (DeclarationDescriptor descriptor : allDescriptors) {
            if (descriptor instanceof CallableMemberDescriptor) {
                CallableMemberDescriptor callableMemberDescriptor = (CallableMemberDescriptor) descriptor;
                callableProtos.add(serializer.callableProto(callableMemberDescriptor).build());
            }
        }
        return callableProtos;
    }

    private static void deserializeCallables(
            List<ProtoBuf.Callable> callableProtos,
            MutablePackageFragmentDescriptor actualPackage,
            NameResolver nameResolver,
            DescriptorFinder descriptorFinder
    ) {
        DescriptorDeserializer descriptorDeserializer =
                DescriptorDeserializer.create(new LockBasedStorageManager(), actualPackage, nameResolver, descriptorFinder, AnnotationDeserializer.UNSUPPORTED);
        for (ProtoBuf.Callable callableProto : callableProtos) {
            CallableMemberDescriptor callableMemberDescriptor = descriptorDeserializer.loadCallable(callableProto);
            if (callableMemberDescriptor instanceof PropertyDescriptor) {
                PropertyDescriptor propertyDescriptor = (PropertyDescriptor) callableMemberDescriptor;
                actualPackage.getMemberScope().addPropertyDescriptor(propertyDescriptor);
            }
            else if (callableMemberDescriptor instanceof FunctionDescriptor) {
                FunctionDescriptor functionDescriptor = (FunctionDescriptor) callableMemberDescriptor;
                actualPackage.getMemberScope().addFunctionDescriptor(functionDescriptor);
            }
        }
    }
}
