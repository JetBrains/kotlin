package org.jetbrains.jet.compiler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.PropertyCodegen;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaNamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.Variance;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Stepan Koltsov
 */
class NamespaceComparator {

    private final boolean includeObject;

    private NamespaceComparator(boolean includeObject) {
        this.includeObject = includeObject;
    }

    public static void compareNamespaces(@NotNull NamespaceDescriptor nsa, @NotNull NamespaceDescriptor nsb, boolean includeObject) {
        new NamespaceComparator(includeObject).doCompareNamespaces(nsa, nsb);
    }

    private void doCompareNamespaces(@NotNull NamespaceDescriptor nsa, @NotNull NamespaceDescriptor nsb) {
        Assert.assertEquals(nsa.getName(), nsb.getName());
        System.out.println("namespace " + nsa.getName());

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
            compareClassifiers(ca, cb);
        }

        for (String name : propertyNames) {
            Set<VariableDescriptor> pa = nsa.getMemberScope().getProperties(name);
            Set<VariableDescriptor> pb = nsb.getMemberScope().getProperties(name);
            compareDeclarationSets(pa, pb);

            Assert.assertTrue(nsb.getMemberScope().getFunctions(PropertyCodegen.getterName(name)).isEmpty());
            Assert.assertTrue(nsb.getMemberScope().getFunctions(PropertyCodegen.setterName(name)).isEmpty());
        }

        for (String name : functionNames) {
            Set<FunctionDescriptor> fa = nsa.getMemberScope().getFunctions(name);
            Set<FunctionDescriptor> fb = nsb.getMemberScope().getFunctions(name);
            compareDeclarationSets(fa, fb);
        }
    }

    private static void compareDeclarationSets(Set<? extends DeclarationDescriptor> a, Set<? extends DeclarationDescriptor> b) {
        String at = serializedDeclarationSets(a);
        String bt = serializedDeclarationSets(b);
        Assert.assertEquals(at, bt);
        System.out.print(at);
    }

    private static String serializedDeclarationSets(Collection<? extends DeclarationDescriptor> ds) {
        List<String> strings = new ArrayList<String>();
        for (DeclarationDescriptor d : ds) {
            StringBuilder sb = new StringBuilder();
            new Serializer(sb).serialize(d);
            strings.add(sb.toString());
        }

        Collections.sort(strings);

        StringBuilder r = new StringBuilder();
        for (String string : strings) {
            r.append(string);
            r.append("\n");
        }
        return r.toString();
    }

    private void compareClassifiers(@NotNull ClassifierDescriptor a, @NotNull ClassifierDescriptor b) {
        StringBuilder sba = new StringBuilder();
        StringBuilder sbb = new StringBuilder();

        new FullContentSerialier(sba).serialize((ClassDescriptor) a);
        new FullContentSerialier(sbb).serialize((ClassDescriptor) b);

        String as = sba.toString();
        String bs = sbb.toString();

        Assert.assertEquals(as, bs);
        System.out.println(as);
    }

    private void compareDescriptors(@NotNull DeclarationDescriptor a, @NotNull DeclarationDescriptor b) {
        StringBuilder sba = new StringBuilder();
        StringBuilder sbb = new StringBuilder();
        new Serializer(sba).serialize(a);
        new Serializer(sbb).serialize(b);

        String as = sba.toString();
        String bs = sbb.toString();

        Assert.assertEquals(as, bs);
        System.out.println(as);
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
                default:
                    throw new IllegalStateException();
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
                new Serializer(sb).serialize(fun.getReceiverParameter());
                sb.append(".");
            }

            sb.append(fun.getName());
            sb.append("(");
            new ValueParameterSerializer(sb).serializeCommaSeparated(fun.getValueParameters());
            sb.append("): ");
            new Serializer(sb).serialize(fun.getReturnType());
        }

        public void serialize(ExtensionReceiver extensionReceiver) {
            serialize(extensionReceiver.getType());
        }

        public void serialize(PropertyDescriptor prop) {
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
                // TODO: print only name for type parameter
                new Serializer(sb).serialize(prop.getReceiverParameter().getType());
                sb.append(".");
            }
            sb.append(prop.getName());
            sb.append(": ");
            new Serializer(sb).serialize(prop.getOutType());
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
                serialize(valueParameter.getVarargElementType());
            } else {
                serialize(valueParameter.getOutType());
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

        public void serialize(JetType type) {
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

        public void serialize(ModuleDescriptor module) {
            // nop
        }

        public void serialize(ClassDescriptor clazz) {
            new NamespacePrefixSerializer(sb).serialize(clazz.getContainingDeclaration());
            sb.append(clazz.getName());
        }

        public void serialize(NamespaceDescriptor ns) {
            if (isRootNs(ns)) {
                return;
            }
            if (ns.getContainingDeclaration() != null) {
                new NamespacePrefixSerializer(sb).serialize(ns.getContainingDeclaration());
            }
            sb.append(ns.getName());
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
                    new ValueParameterSerializer(sb).serialize(upper);
                    list.add(sb.toString());
                }
                Collections.sort(list);
                serializeSeparated(list, " & "); // TODO: use where
            }
            // TODO: lower bounds
        }

    }

    private static boolean isRootNs(DeclarationDescriptor ns) {
        // upyachka
        return ns instanceof JavaNamespaceDescriptor && JavaDescriptorResolver.JAVA_ROOT.equals(ns.getName());
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

    private static class ValueParameterSerializer extends Serializer {

        public ValueParameterSerializer(StringBuilder sb) {
            super(sb);
        }

        @Override
        public void serialize(TypeParameterDescriptor param) {
            sb.append(param.getName());
        }
    }

    private class FullContentSerialier extends Serializer {
        private FullContentSerialier(StringBuilder sb) {
            super(sb);
        }

        public void serialize(ClassDescriptor klass) {

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
                new Serializer(sb).serializeCommaSeparated(new ArrayList<JetType>(klass.getTypeConstructor().getSupertypes()));
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

            Collections.sort(memberStrings);

            for (String memberString : memberStrings) {
                sb.append(indent(memberString));
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
