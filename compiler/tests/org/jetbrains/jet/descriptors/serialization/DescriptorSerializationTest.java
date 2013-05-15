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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNullable;
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNullableImpl;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.renderer.DescriptorRendererBuilder;
import org.jetbrains.jet.utils.Printer;
import org.jetbrains.jet.utils.Profiler;

import java.io.*;
import java.util.*;

public class DescriptorSerializationTest extends KotlinTestWithEnvironment {

    private static final DescriptorRenderer RENDERER = new DescriptorRendererBuilder()
            .setVerbose(true)
            .build();

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    public void testBuiltins() throws Exception {
        doTest(KotlinBuiltIns.getInstance().getBuiltInsScope().getAllDescriptors());
    }

    public void testBooleanIterator() throws Exception {
        ClassifierDescriptor classifier = KotlinBuiltIns.getInstance().getBuiltInsScope().getClassifier(Name.identifier("BooleanIterator"));
        doTest(Collections.<DeclarationDescriptor>singletonList(classifier));
    }

    private static void doTest(Collection<DeclarationDescriptor> initial) throws IOException {
        List<DeclarationDescriptor> resulting = getDeserializedDescriptors(initial);

        String actualText = toText(resulting);
        String expectedText = toText(initial);

        assertEquals(expectedText, actualText);
    }

    private static List<DeclarationDescriptor> getDeserializedDescriptors(Collection<DeclarationDescriptor> descriptors) throws IOException {
        Profiler serialize = Profiler.create("serialize").start();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serialize(descriptors, out);

        serialize.end();

        System.out.println("Size in bytes: " + out.size());

        Profiler deserialize = Profiler.create("deserialize").start();

        InputStream in = new ByteArrayInputStream(out.toByteArray());

        ProtoBuf.SimpleNameTable simpleNames = ProtoBuf.SimpleNameTable.parseDelimitedFrom(in);
        ProtoBuf.QualifiedNameTable qualifiedNames = ProtoBuf.QualifiedNameTable.parseDelimitedFrom(in);
        List<ProtoBuf.Class> classProtos = parseClasses(in);

        ClassResolverImpl classResolver = new ClassResolverImpl(
                new ClassResolver() {
                    @Nullable
                    @Override
                    public ClassDescriptor findClass(@NotNull FqName fqName) {
                        while (!fqName.isRoot()) {
                            NamespaceDescriptor namespace = KotlinBuiltIns.getInstance().getBuiltInsModule().getNamespace(fqName.parent());
                            if (namespace == null) {
                                fqName = fqName.parent();
                            }
                            else {
                                return (ClassDescriptor) namespace.getMemberScope().getClassifier(fqName.shortName());
                            }
                        }
                        return null;
                    }
                }, KotlinBuiltIns.getInstance().getBuiltInsPackage(), simpleNames, qualifiedNames, classProtos
        );

        List<DeclarationDescriptor> result = new ArrayList<DeclarationDescriptor>();
        for (ProtoBuf.Class classProto : classProtos) {
            ClassDescriptor classDescriptor =
                    classResolver.findClass(new FqName("jet." + classResolver.getNameResolver().getName(classProto.getName())));
            result.add(classDescriptor);
        }

        deserialize.end();

        return result;
    }

    public static void serialize(
            @NotNull Collection<? extends DeclarationDescriptor> descriptors,
            @NotNull OutputStream out
    ) throws IOException {
        DescriptorSerializer descriptorSerializer = new DescriptorSerializer();

        List<MessageLite> messages = new ArrayList<MessageLite>();
        for (DeclarationDescriptor descriptor : descriptors) {
            if (descriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
                messages.add(descriptorSerializer.classProto(classDescriptor).build());

            }
        }

        NameSerializationUtil.serializeNameTable(out, descriptorSerializer.getNameTable());

        for (MessageLite message : messages) {
            message.writeDelimitedTo(out);
        }
    }

    private static List<ProtoBuf.Class> parseClasses(InputStream in) throws IOException {
        List<ProtoBuf.Class> classProtos = new ArrayList<ProtoBuf.Class>();
        while (true) {
            ProtoBuf.Class classProto = ProtoBuf.Class.parseDelimitedFrom(in);
            if (classProto == null) break;
            classProtos.add(classProto);
        }
        return classProtos;
    }


    private static String toText(Collection<DeclarationDescriptor> resulting) {
        StringBuilder resultingBuilder = new StringBuilder();
        printSorted(resulting, new Printer(resultingBuilder));

        return resultingBuilder.toString();
    }

    private static void printSorted(Collection<DeclarationDescriptor> descriptors, Printer p) {
        Comparator<DeclarationDescriptor> comparator = new Comparator<DeclarationDescriptor>() {
            @Override
            public int compare(
                    DeclarationDescriptor o1, DeclarationDescriptor o2
            ) {
                int names = o1.getName().getName().compareTo(o2.getName().getName());
                if (names != 0) return names;
                return RENDERER.render(o1).compareTo(RENDERER.render(o2));
            }
        };

        List<DeclarationDescriptor> sortedDescriptors = Lists.newArrayList(descriptors);
        Collections.sort(sortedDescriptors, comparator);

        for (DeclarationDescriptor descriptor : sortedDescriptors) {
            p.println(RENDERER.render(descriptor));

            if (descriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
                p.pushIndent();
                List<DeclarationDescriptor> members = Lists.newArrayList(
                        classDescriptor.getDefaultType().getMemberScope().getAllDescriptors());
                Collections.sort(members, comparator);
                for (DeclarationDescriptor member : members) {
                    p.println(RENDERER.render(member));
                }
                p.popIndent();
            }
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
}
