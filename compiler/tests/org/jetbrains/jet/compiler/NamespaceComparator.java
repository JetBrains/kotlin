/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.compiler;

import com.google.common.io.Files;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.PropertyCodegen;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.Variance;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author Stepan Koltsov
 */
class NamespaceComparator {

    private final boolean includeObject;

    private NamespaceComparator(boolean includeObject) {
        this.includeObject = includeObject;
    }

    public static void compareNamespaces(@NotNull NamespaceDescriptor nsa, @NotNull NamespaceDescriptor nsb, boolean includeObject,
            @NotNull File txtFile) {
        String serialized = new NamespaceComparator(includeObject).doCompareNamespaces(nsa, nsb);
        try {
            for (;;) {
                String expected = Files.toString(txtFile, Charset.forName("utf-8")).replace("\r\n", "\n");

                if (expected.contains("kick me")) {
                    // for developer
                    System.err.println("generating " + txtFile);
                    Files.write(serialized, txtFile, Charset.forName("utf-8"));
                    continue;
                }

                // compare with hardcopy: make sure nothing is lost in output
                Assert.assertEquals(expected, serialized);
                break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String doCompareNamespaces(@NotNull NamespaceDescriptor nsa, @NotNull NamespaceDescriptor nsb) {
        StringBuilder sb = new StringBuilder();
        Assert.assertEquals(nsa.getName(), nsb.getName());

        sb.append("namespace " + nsa.getName() + "\n\n");

        Assert.assertTrue(!nsa.getMemberScope().getAllDescriptors().isEmpty());

        Set<String> classifierNames = new HashSet<String>();
        Set<String> propertyNames = new HashSet<String>();
        Set<String> functionNames = new HashSet<String>();

        for (DeclarationDescriptor ad : nsa.getMemberScope().getAllDescriptors()) {
            if (ad instanceof ClassifierDescriptor) {
                classifierNames.add(ad.getName());
            } else if (ad instanceof PropertyDescriptor) {
                propertyNames.add(ad.getName());
            } else if (ad instanceof FunctionDescriptor) {
                functionNames.add(ad.getName());
            } else {
                throw new AssertionError("unknown member: " + ad);
            }
        }

        for (String name : classifierNames) {
            ClassifierDescriptor ca = nsa.getMemberScope().getClassifier(name);
            ClassifierDescriptor cb = nsb.getMemberScope().getClassifier(name);
            Assert.assertTrue(ca != null);
            Assert.assertTrue(cb != null);
            compareClassifiers(ca, cb, sb);
        }

        for (String name : propertyNames) {
            Set<VariableDescriptor> pa = nsa.getMemberScope().getProperties(name);
            Set<VariableDescriptor> pb = nsb.getMemberScope().getProperties(name);
            compareDeclarationSets(pa, pb, sb);

            Assert.assertTrue(nsb.getMemberScope().getFunctions(PropertyCodegen.getterName(name)).isEmpty());
            Assert.assertTrue(nsb.getMemberScope().getFunctions(PropertyCodegen.setterName(name)).isEmpty());
        }

        for (String name : functionNames) {
            Set<FunctionDescriptor> fa = nsa.getMemberScope().getFunctions(name);
            Set<FunctionDescriptor> fb = nsb.getMemberScope().getFunctions(name);
            compareDeclarationSets(fa, fb, sb);
        }

        return sb.toString();
    }

    private static void compareDeclarationSets(Set<? extends DeclarationDescriptor> a, Set<? extends DeclarationDescriptor> b,
            @NotNull StringBuilder sb) {
        String at = serializedDeclarationSets(a);
        String bt = serializedDeclarationSets(b);
        Assert.assertEquals(at, bt);
        sb.append(at);
    }

    private static String serializedDeclarationSets(Collection<? extends DeclarationDescriptor> ds) {
        List<String> strings = new ArrayList<String>();
        for (DeclarationDescriptor d : ds) {
            StringBuilder sb = new StringBuilder();
            new Serializer(sb).serialize(d);
            strings.add(sb.toString());
        }

        Collections.sort(strings, new MemberComparator());

        StringBuilder r = new StringBuilder();
        for (String string : strings) {
            r.append(string);
            r.append("\n");
        }
        return r.toString();
    }

    /**
     * This comparator only affects test output, you can drop it if you don't want to understand it.
     */
    private static class MemberComparator implements Comparator<String> {

        @NotNull
        private String normalize(String s) {
            return s.replaceFirst(
                    "^ *(private|final|abstract|open|override|fun|val|var|/\\*.*?\\*/|((?!<init>)<.*?>)| )*",
                    "")
               + s;
        }

        @Override
        public int compare(@NotNull String a, @NotNull String b) {
            return normalize(a).compareTo(normalize(b));
        }
    }

    private void compareClassifiers(@NotNull ClassifierDescriptor a, @NotNull ClassifierDescriptor b, @NotNull StringBuilder sb) {
        StringBuilder sba = new StringBuilder();
        StringBuilder sbb = new StringBuilder();

        new FullContentSerialier(sba).serialize((ClassDescriptor) a);
        new FullContentSerialier(sbb).serialize((ClassDescriptor) b);

        String as = sba.toString();
        String bs = sbb.toString();

        Assert.assertEquals(as, bs);
        sb.append(as);
    }



    private static class Serializer {

        protected final StringBuilder sb;

        public Serializer(StringBuilder sb) {
            this.sb = sb;
        }

        public void serialize(ClassKind kind) {
            switch (kind) {
                case CLASS:
                    sb.append("class");
                    break;
                case TRAIT:
                    sb.append("trait");
                    break;
                case OBJECT:
                    sb.append("object");
                    break;
                case ANNOTATION_CLASS:
                    sb.append("annotation class");
                    break;
                default:
                    throw new IllegalStateException("unknown class kind: " + kind);
            }
        }


        private static Object invoke(Method method, Object thiz, Object... args) {
            try {
                return method.invoke(thiz, args);
            } catch (Exception e) {
                throw new RuntimeException("failed to invoke " + method + ": " + e, e);
            }
        }


        public void serialize(FunctionDescriptor fun) {
            serialize(fun.getModality());
            sb.append(" ");

            if (!fun.getAnnotations().isEmpty()) {
                new Serializer(sb).serializeSeparated(fun.getAnnotations(), " ");
                sb.append(" ");
            }

            if (!fun.getOverriddenDescriptors().isEmpty()) {
                sb.append("override /*" + fun.getOverriddenDescriptors().size() + "*/ ");
            }

            if (fun instanceof ConstructorDescriptor) {
                sb.append("/*constructor*/ ");
            }
            sb.append("fun ");
            if (!fun.getTypeParameters().isEmpty()) {
                sb.append("<");
                new Serializer(sb).serializeCommaSeparated(fun.getTypeParameters());
                sb.append(">");
            }

            if (fun.getReceiverParameter().exists()) {
                new TypeSerializer(sb).serialize(fun.getReceiverParameter());
                sb.append(".");
            }

            sb.append(fun.getName());
            sb.append("(");
            new TypeSerializer(sb).serializeCommaSeparated(fun.getValueParameters());
            sb.append("): ");
            new TypeSerializer(sb).serialize(fun.getReturnType());
        }

        public void serialize(ExtensionReceiver extensionReceiver) {
            serialize(extensionReceiver.getType());
        }

        public void serialize(PropertyDescriptor prop) {
            serialize(prop.getModality());
            sb.append(" ");

            if (!prop.getAnnotations().isEmpty()) {
                new Serializer(sb).serializeSeparated(prop.getAnnotations(), " ");
                sb.append(" ");
            }

            if (prop.isVar()) {
                sb.append("var ");
            } else {
                sb.append("val ");
            }
            if (!prop.getTypeParameters().isEmpty()) {
                sb.append(" <");
                new Serializer(sb).serializeCommaSeparated(prop.getTypeParameters());
                sb.append("> ");
            }
            if (prop.getReceiverParameter().exists()) {
                new TypeSerializer(sb).serialize(prop.getReceiverParameter().getType());
                sb.append(".");
            }
            sb.append(prop.getName());
            sb.append(": ");
            new TypeSerializer(sb).serialize(prop.getType());
        }

        public void serialize(ValueParameterDescriptor valueParameter) {
            sb.append("/*");
            sb.append(valueParameter.getIndex());
            sb.append("*/ ");
            if (valueParameter.getVarargElementType() != null) {
                sb.append("vararg ");
            }
            sb.append(valueParameter.getName());
            sb.append(": ");
            if (valueParameter.getVarargElementType() != null) {
                new TypeSerializer(sb).serialize(valueParameter.getVarargElementType());
            } else {
                new TypeSerializer(sb).serialize(valueParameter.getType());
            }
            if (valueParameter.hasDefaultValue()) {
                sb.append(" = ?");
            }
        }

        public void serialize(Variance variance) {
            if (variance == Variance.INVARIANT) {

            } else {
                sb.append(variance);
                sb.append(' ');
            }
        }

        public void serialize(Modality modality) {
            sb.append(modality.name().toLowerCase());
        }

        public void serialize(AnnotationDescriptor annotation) {
            new TypeSerializer(sb).serialize(annotation.getType());
            sb.append("(");
            serializeCommaSeparated(annotation.getValueArguments());
            sb.append(")");
        }

        public void serializeCommaSeparated(List<?> list) {
            serializeSeparated(list, ", ");
        }

        public void serializeSeparated(List<?> list, String sep) {
            boolean first = true;
            for (Object o : list) {
                if (!first) {
                    sb.append(sep);
                }
                serialize(o);
                first = false;
            }
        }

        private Method getMethodToSerialize(Object o) {
            if (o == null) {
                throw new IllegalStateException("won't serialize null");
            }

            // TODO: cache
            for (Method method : this.getClass().getMethods()) {
                if (!method.getName().equals("serialize")) {
                    continue;
                }
                if (method.getParameterTypes().length != 1) {
                    continue;
                }
                if (method.getParameterTypes()[0].equals(Object.class)) {
                    continue;
                }
                if (method.getParameterTypes()[0].isInstance(o)) {
                    method.setAccessible(true);
                    return method;
                }
            }
            throw new IllegalStateException("don't know how to serialize " + o + " (of " + o.getClass() + ")");
        }

        public void serialize(Object o) {
            Method method = getMethodToSerialize(o);
            invoke(method, this, o);
        }

        public void serialize(String s) {
            sb.append(s);
        }

        public void serialize(ClassDescriptor clazz) {
            sb.append(DescriptorUtils.getFQName(clazz));
        }

        public void serialize(NamespaceDescriptor ns) {
            sb.append(DescriptorUtils.getFQName(ns));
        }

        public void serialize(TypeParameterDescriptor param) {
            sb.append("/*");
            sb.append(param.getIndex());
            if (param.isReified()) {
                sb.append(",r");
            }
            sb.append("*/ ");
            serialize(param.getVariance());
            sb.append(param.getName());
            if (!param.getUpperBounds().isEmpty()) {
                sb.append(" : ");
                List<String> list = new ArrayList<String>();
                for (JetType upper : param.getUpperBounds()) {
                    StringBuilder sb = new StringBuilder();
                    new TypeSerializer(sb).serialize(upper);
                    list.add(sb.toString());
                }
                Collections.sort(list);
                serializeSeparated(list, " & "); // TODO: use where
            }
            // TODO: lower bounds
        }

    }
    
    private static class TypeSerializer extends Serializer {

        public TypeSerializer(StringBuilder sb) {
            super(sb);
        }

        @Override
        public void serialize(TypeParameterDescriptor param) {
            sb.append(param.getName());
        }

        public void serialize(@NotNull JetType type) {
            serialize(type.getConstructor().getDeclarationDescriptor());
            if (!type.getArguments().isEmpty()) {
                sb.append("<");
                boolean first = true;
                for (TypeProjection proj : type.getArguments()) {
                    if (!first) {
                        sb.append(", ");
                    }
                    serialize(proj.getProjectionKind());
                    serialize(proj.getType());
                    first = false;
                }
                sb.append(">");
            }
            if (type.isNullable()) {
                sb.append("?");
            }
        }

    }

    private static boolean isRootNs(DeclarationDescriptor ns) {
        return ns instanceof NamespaceDescriptor && ns.getContainingDeclaration() instanceof ModuleDescriptor;
    }

    private static class NamespacePrefixSerializer extends Serializer {

        public NamespacePrefixSerializer(StringBuilder sb) {
            super(sb);
        }

        @Override
        public void serialize(NamespaceDescriptor ns) {
            super.serialize(ns);
            if (isRootNs(ns)) {
                return;
            }
            sb.append(".");
        }

        @Override
        public void serialize(ClassDescriptor clazz) {
            super.serialize(clazz);
            sb.append(".");
        }
    }

    private class FullContentSerialier extends Serializer {
        private FullContentSerialier(StringBuilder sb) {
            super(sb);
        }

        public void serialize(ClassDescriptor klass) {

            if (!klass.getAnnotations().isEmpty()) {
                new Serializer(sb).serializeSeparated(klass.getAnnotations(), " ");
                sb.append(" ");
            }
            serialize(klass.getModality());
            sb.append(" ");

            serialize(klass.getKind());
            sb.append(" ");

            new Serializer(sb).serialize(klass);

            if (!klass.getTypeConstructor().getParameters().isEmpty()) {
                sb.append("<");
                serializeCommaSeparated(klass.getTypeConstructor().getParameters());
                sb.append(">");
            }

            if (!klass.getTypeConstructor().getSupertypes().isEmpty()) {
                sb.append(" : ");
                new TypeSerializer(sb).serializeCommaSeparated(new ArrayList<JetType>(klass.getTypeConstructor().getSupertypes()));
            }

            sb.append(" {\n");

            List<TypeProjection> typeArguments = new ArrayList<TypeProjection>();
            for (TypeParameterDescriptor param : klass.getTypeConstructor().getParameters()) {
                typeArguments.add(new TypeProjection(Variance.INVARIANT, param.getDefaultType()));
            }

            List<String> memberStrings = new ArrayList<String>();

            for (ConstructorDescriptor constructor : klass.getConstructors()) {
                StringBuilder constructorSb = new StringBuilder();
                new Serializer(constructorSb).serialize(constructor);
                memberStrings.add(constructorSb.toString());
            }

            JetScope memberScope = klass.getMemberScope(typeArguments);
            for (DeclarationDescriptor member : memberScope.getAllDescriptors()) {
                if (!includeObject) {
                    if (member.getName().matches("equals|hashCode|finalize|wait|notify(All)?|toString|clone|getClass")) {
                        continue;
                    }
                }
                StringBuilder memberSb = new StringBuilder();
                new FullContentSerialier(memberSb).serialize(member);
                memberStrings.add(memberSb.toString());
            }

            Collections.sort(memberStrings, new MemberComparator());

            for (String memberString : memberStrings) {
                sb.append(indent(memberString));
            }

            if (klass.getClassObjectDescriptor() != null) {
                StringBuilder sbForClassObject = new StringBuilder();
                new FullContentSerialier(sbForClassObject).serialize(klass.getClassObjectDescriptor());
                sb.append(indent(sbForClassObject.toString()));
            }

            sb.append("}\n");
        }
    }


    private static String indent(String string) {
        try {
            StringBuilder r = new StringBuilder();
            BufferedReader reader = new BufferedReader(new StringReader(string));
            for (;;) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                r.append("    ");
                r.append(line);
                r.append("\n");
            }
            return r.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
