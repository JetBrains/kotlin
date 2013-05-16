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
import com.google.protobuf.MessageLite;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JavaBridgeConfiguration;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.lazy.LazyResolveTestUtil;
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNullable;
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNullableImpl;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.test.util.NamespaceComparator;

import java.io.*;
import java.util.*;

public abstract class AbstractDescriptorSerializationTest extends KotlinTestWithEnvironment {

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    protected void doTest(String path) throws IOException {
        File ktFile = new File(path);
        ModuleDescriptor moduleDescriptor = LazyResolveTestUtil.resolveEagerly(Collections.singletonList(
                JetTestUtils.createFile(ktFile.getName(), FileUtil.loadFile(ktFile), getProject())
        ), getEnvironment());

        NamespaceDescriptor testNamespace = moduleDescriptor.getNamespace(FqName.topLevel(Name.identifier("test")));
        assert testNamespace != null;

        JavaDescriptorResolver javaDescriptorResolver = new InjectorForJavaDescriptorResolver(
                getProject(), new BindingTraceContext(), moduleDescriptor).getJavaDescriptorResolver();

        Collection<DeclarationDescriptor> initial = testNamespace.getMemberScope().getAllDescriptors();

        NamespaceDescriptor deserialized = serializeAndDeserialize(javaDescriptorResolver, initial);

        NamespaceComparator.validateAndCompareNamespaces(testNamespace, deserialized, NamespaceComparator.RECURSIVE, null);
    }

    private static NamespaceDescriptor serializeAndDeserialize(
            JavaDescriptorResolver javaDescriptorResolver,
            Collection<DeclarationDescriptor> initial
    ) throws IOException {
        List<ClassDescriptor> classes = Lists.newArrayList();
        List<CallableMemberDescriptor> callables = Lists.newArrayList();
        for (DeclarationDescriptor descriptor : initial) {
            if (descriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
                classes.add(classDescriptor);
            }
            else if (descriptor instanceof CallableMemberDescriptor) {
                CallableMemberDescriptor memberDescriptor = (CallableMemberDescriptor) descriptor;
                callables.add(memberDescriptor);
            }
            else {
                fail("Unsupported descriptor type: " + descriptor);
            }
        }
        return getDeserializedDescriptors(javaDescriptorResolver, classes, callables);
    }

    private static NamespaceDescriptor getDeserializedDescriptors(
            final JavaDescriptorResolver javaDescriptorResolver,
            List<ClassDescriptor> classes,
            List<CallableMemberDescriptor> callables
    ) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serialize(classes, callables, out);

        InputStream in = new ByteArrayInputStream(out.toByteArray());

        ProtoBuf.SimpleNameTable simpleNames = ProtoBuf.SimpleNameTable.parseDelimitedFrom(in);
        ProtoBuf.QualifiedNameTable qualifiedNames = ProtoBuf.QualifiedNameTable.parseDelimitedFrom(in);

        List<ProtoBuf.Class> classProtos = Lists.newArrayList();
        List<ProtoBuf.Callable> callableProtos = Lists.newArrayList();

        for (int i = 0; i < classes.size(); i++) {
            ProtoBuf.Class proto = ProtoBuf.Class.parseDelimitedFrom(in);
            classProtos.add(proto);
        }

        for (int i = 0; i < callables.size(); i++) {
            ProtoBuf.Callable proto = ProtoBuf.Callable.parseDelimitedFrom(in);
            callableProtos.add(proto);
        }

        NamespaceDescriptorImpl namespace = createTestNamespace();
        ClassResolver javaClassResolver = new JavaClassResolver(javaDescriptorResolver);

        ClassResolverImpl classResolver = new ClassResolverImpl(
                javaClassResolver, namespace, simpleNames, qualifiedNames, classProtos
        );

        for (ProtoBuf.Class proto : classProtos) {
            FqName fqName = new FqName("test." + classResolver.getNameResolver().getName(proto.getName()));
            ClassDescriptor descriptor = classResolver.findClass(fqName);
            assert descriptor != null : "Class not loaded: " + fqName;
            namespace.getMemberScope().addClassifierDescriptor(descriptor);
        }

        DescriptorDeserializer deserializer =
                new DescriptorDeserializer((DescriptorDeserializer) null, namespace, new NameResolver(simpleNames, qualifiedNames, classResolver));
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

    private static NamespaceDescriptorImpl createTestNamespace() {
        ModuleDescriptorImpl module = new ModuleDescriptorImpl(Name.special("<name>"), JavaBridgeConfiguration.ALL_JAVA_IMPORTS,
                                                                   JavaToKotlinClassMap.getInstance());
        NamespaceDescriptorImpl rootNamespace =
                new NamespaceDescriptorImpl(module, Collections.<AnnotationDescriptor>emptyList(), JetPsiUtil.ROOT_NAMESPACE_NAME);
        module.setRootNamespace(rootNamespace);
        NamespaceDescriptorImpl test =
                new NamespaceDescriptorImpl(rootNamespace, Collections.<AnnotationDescriptor>emptyList(), Name.identifier("test"));
        test.initialize(new WritableScopeImpl(JetScope.EMPTY, test, RedeclarationHandler.DO_NOTHING, "members of test namespace"));
        return test;
    }

    public static void serialize(
            List<ClassDescriptor> classes, List<CallableMemberDescriptor> callables, @NotNull OutputStream out
    ) throws IOException {
        DescriptorSerializer descriptorSerializer = new DescriptorSerializer();

        List<MessageLite> messages = Lists.newArrayList();
        for (ClassDescriptor classDescriptor : classes) {
            messages.add(descriptorSerializer.classProto(classDescriptor).build());

        }

        for (CallableMemberDescriptor callable : callables) {
            messages.add(descriptorSerializer.callableProto(callable).build());
        }

        NameSerializationUtil.serializeNameTable(out, descriptorSerializer.getNameTable());

        for (MessageLite message : messages) {
            message.writeDelimitedTo(out);
        }
    }

    private static class ClassResolverImpl implements ClassResolver {
        private final ClassResolver parentResolver;

        private final DeclarationDescriptor parentForClasses;
        private final FqName parentFqName;
        private final NameResolver nameResolver;
        private final Map<Name, ProtoBuf.Class> classProtos;

        private final MemoizedFunctionToNullable<FqName, ClassDescriptor> classes;

        public ClassResolverImpl(
                @NotNull ClassResolver parentResolver,
                @NotNull DeclarationDescriptor parentForClasses,
                @NotNull ProtoBuf.SimpleNameTable simpleNames,
                @NotNull ProtoBuf.QualifiedNameTable qualifiedNames,
                @NotNull List<ProtoBuf.Class> classProtos
        ) {
            this.parentResolver = parentResolver;

            this.parentForClasses = parentForClasses;
            this.parentFqName = DescriptorUtils.getFQName(parentForClasses).toSafe();

            this.nameResolver = new NameResolver(simpleNames, qualifiedNames, this);
            this.classProtos = toMap(classProtos);

            this.classes = new MemoizedFunctionToNullableImpl<FqName, ClassDescriptor>() {
                @Nullable
                @Override
                protected ClassDescriptor doCompute(@NotNull FqName fqName) {
                    return resolveClass(fqName);
                }
            };
        }

        @NotNull
        private Map<Name, ProtoBuf.Class> toMap(@NotNull List<ProtoBuf.Class> classProtos) {
            Map<Name, ProtoBuf.Class> map = new HashMap<Name, ProtoBuf.Class>(classProtos.size());
            for (ProtoBuf.Class classProto : classProtos) {
                map.put(nameResolver.getName(classProto.getName()), classProto);
            }
            return map;
        }

        @NotNull
        public NameResolver getNameResolver() {
            return nameResolver;
        }

        @Nullable
        private ClassDescriptor resolveClass(@NotNull FqName fqName) {
            if (!parentFqName.equals(fqName.parent())) {
                return parentResolver.findClass(fqName);
            }

            ProtoBuf.Class classProto = classProtos.get(fqName.shortName());
            if (classProto == null) {
                return parentResolver.findClass(fqName);
            }

            return new DeserializedClassDescriptor(parentForClasses, nameResolver, this, classProto, null);
        }

        @Nullable
        @Override
        public ClassDescriptor findClass(@NotNull FqName fqName) {
            return classes.fun(fqName);
        }
    }

    private static class JavaClassResolver implements ClassResolver {
        private final JavaDescriptorResolver javaDescriptorResolver;

        public JavaClassResolver(JavaDescriptorResolver javaDescriptorResolver) {
            this.javaDescriptorResolver = javaDescriptorResolver;
        }

        @Nullable
        @Override
        public ClassDescriptor findClass(@NotNull FqName fqName) {
            ClassDescriptor classDescriptor = javaDescriptorResolver.resolveClass(fqName);
            if (classDescriptor != null) {
                return classDescriptor;
            }
            FqName current = fqName;
            while (!current.isRoot()) {
                NamespaceDescriptor namespace = getNamespace(current);
                if (namespace != null) {
                    ClassDescriptor classifier = (ClassDescriptor) namespace.getMemberScope().getClassifier(current.shortName());
                    if (classifier == null) {
                        throw new IllegalStateException("Class not found: " + fqName);
                    }
                    return classifier;
                }
                current = current.parent();
            }
            throw new IllegalStateException("Class not found: " + fqName);
        }

        @Nullable
        private NamespaceDescriptor getNamespace(@NotNull FqName fqName) {
            return javaDescriptorResolver.resolveNamespace(fqName);
        }
    }
}
