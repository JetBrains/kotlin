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
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorImpl;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.renderer.DescriptorRendererBuilder;
import org.jetbrains.jet.test.util.NamespaceComparator;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.descriptors.serialization.ClassSerializationUtil.constantSerializer;

public class BuiltinsDeserializationTest extends KotlinTestWithEnvironment {
    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithJdk(ConfigurationKind.JDK_AND_ANNOTATIONS, TestJdkKind.FULL_JDK);
    }

    public void testBuiltIns() throws Exception {
        Collection<DeclarationDescriptor> allDescriptors = KotlinBuiltIns.getInstance().getBuiltInsScope().getAllDescriptors();
        NamespaceDescriptorImpl actualNamespace = getDeserializedDescriptorsAsNamespace(allDescriptors);

        NamespaceComparator.Configuration configuration = NamespaceComparator.RECURSIVE.withRenderer(
                new DescriptorRendererBuilder()
                        .setWithDefinedIn(false)
                        .setExcludedAnnotationClasses(Arrays.asList(new FqName(ExpectedLoadErrorsUtil.ANNOTATION_CLASS_NAME)))
                        .setOverrideRenderingPolicy(DescriptorRenderer.OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE)
                        .setVerbose(true)
                        .setAlwaysRenderAny(true)
                        .setPrettyFunctionTypes(false)
                        .build()
        );
        NamespaceComparator.validateAndCompareNamespaces(KotlinBuiltIns.getInstance().getBuiltInsPackage(), actualNamespace, configuration, null);
    }

    private static NamespaceDescriptorImpl getDeserializedDescriptorsAsNamespace(Collection<DeclarationDescriptor> allDescriptors) {
        DescriptorSerializer serializer = new DescriptorSerializer();

        final Map<ClassId, ProtoBuf.Class> classProtos = serializeClasses(serializer, allDescriptors);

        List<ProtoBuf.Callable> callableProtos = serializeCallables(serializer, allDescriptors);

        final NamespaceDescriptorImpl actualNamespace = JetTestUtils
                .createTestNamespace(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.shortName().asString());

        final NameResolver nameResolver = NameSerializationUtil.createNameResolver(serializer.getNameTable());

        DescriptorFinder descriptorFinder = new AbstractDescriptorFinder(new LockBasedStorageManager(),
                                                                                     AnnotationDeserializer.UNSUPPORTED) {

            @NotNull
            @Override
            protected NamespaceDescriptor getPackage(@NotNull FqName fqName) {
                assert fqName.equals(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME) : "Unsupported package: " + fqName;
                return actualNamespace;
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
                        actualNamespace.getMemberScope().addClassifierDescriptor(classDescriptor);
                        break;
                    case OBJECT:
                        actualNamespace.getMemberScope().addObjectDescriptor(classDescriptor);
                        break;
                    case ENUM_ENTRY:
                        assert false : "Enum entry appears to be a top-level declaration: " + classDescriptor;
                    case CLASS_OBJECT:
                        assert false : "Call object appears to be a top-level declaration: " + classDescriptor;
                }
            }
        };

        // Make the lazy loader create classes
        for (ClassId classId : classProtos.keySet()) {
            descriptorFinder.findClass(classId);
        }

        deserializeCallables(callableProtos, actualNamespace, nameResolver, descriptorFinder);

        actualNamespace.getMemberScope().changeLockLevel(WritableScope.LockLevel.READING);
        return actualNamespace;
    }

    private static Map<ClassId, ProtoBuf.Class> serializeClasses(
            DescriptorSerializer serializer,
            Collection<DeclarationDescriptor> allDescriptors
    ) {
        final Map<ClassId, ProtoBuf.Class> classProtos = Maps.newHashMap();
        ClassSerializationUtil.serializeClasses(allDescriptors, constantSerializer(serializer), new ClassSerializationUtil.Sink() {
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
            NamespaceDescriptorImpl actualNamespace,
            NameResolver nameResolver,
            DescriptorFinder descriptorFinder
    ) {
        DescriptorDeserializer descriptorDeserializer;
        descriptorDeserializer =
                DescriptorDeserializer.create(new LockBasedStorageManager(), actualNamespace, nameResolver, descriptorFinder, AnnotationDeserializer.UNSUPPORTED);
        for (ProtoBuf.Callable callableProto : callableProtos) {
            CallableMemberDescriptor callableMemberDescriptor = descriptorDeserializer.loadCallable(callableProto);
            if (callableMemberDescriptor instanceof PropertyDescriptor) {
                PropertyDescriptor propertyDescriptor = (PropertyDescriptor) callableMemberDescriptor;
                actualNamespace.getMemberScope().addPropertyDescriptor(propertyDescriptor);
            }
            else if (callableMemberDescriptor instanceof FunctionDescriptor) {
                FunctionDescriptor functionDescriptor = (FunctionDescriptor) callableMemberDescriptor;
                actualNamespace.getMemberScope().addFunctionDescriptor(functionDescriptor);
            }
        }
    }
}
