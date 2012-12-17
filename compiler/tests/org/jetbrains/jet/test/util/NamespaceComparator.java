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

package org.jetbrains.jet.test.util;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.PropertyCodegen;
import org.jetbrains.jet.jvm.compiler.ExpectedLoadErrorsUtil;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.junit.Assert;
import org.junit.ComparisonFailure;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Stepan Koltsov
 */
public class NamespaceComparator {

    public static void compareNamespaces(
            @NotNull NamespaceDescriptor expectedNamespace,
            @NotNull NamespaceDescriptor actualNamespace,
            @NotNull Configuration configuration,
            @NotNull File txtFile
    ) {
        String serializedFormWithDemarkedNames = assertNamespacesEqual(expectedNamespace, actualNamespace, configuration);
        // The serializer puts "!" in front of the name because it allows for speedy sorting of members
        // see MemberComparator.normalize()
        String serialized = serializedFormWithDemarkedNames.replace("!", "");
        try {
            if (!txtFile.exists()) {
                FileUtil.writeToFile(txtFile, serialized);
                Assert.fail("Expected data file did not exist. Generating: " + txtFile);
            }
            String expected = FileUtil.loadFile(txtFile, true);

            // compare with hardcopy: make sure nothing is lost in output
            Assert.assertEquals(expected, serialized);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String assertNamespacesEqual(
            NamespaceDescriptor expectedNamespace,
            NamespaceDescriptor actualNamespace,
            @NotNull Configuration configuration
    ) {
        NamespaceComparator comparator = new NamespaceComparator(configuration);
        String serialized = comparator.doCompareNamespaces(expectedNamespace, actualNamespace);
        comparator.deferred.throwFailures();
        return serialized;
    }

    public static final Configuration DONT_INCLUDE_METHODS_OF_OBJECT = new Configuration(false, false, Predicates.<FqNameUnsafe>alwaysTrue(), Predicates.<NamespaceDescriptor>alwaysTrue());
    public static final Configuration RECURSIVE = new Configuration(false, true, Predicates.<FqNameUnsafe>alwaysTrue(), Predicates.<NamespaceDescriptor>alwaysTrue());
    public static final Configuration NON_RECURSIVE = new Configuration(false, true, Predicates.<FqNameUnsafe>alwaysFalse(), Predicates.<NamespaceDescriptor>alwaysTrue());

    public static class Configuration {

        private final boolean checkPrimaryConstructors;
        private final boolean includeObject;
        private final Predicate<FqNameUnsafe> recurseIntoPackage;
        private final Predicate<NamespaceDescriptor> includeIntoOutput;

        public Configuration(
                boolean checkPrimaryConstructors,
                boolean includeObject,
                Predicate<FqNameUnsafe> recurseIntoPackage,
                Predicate<NamespaceDescriptor> includeIntoOutput
        ) {
            this.checkPrimaryConstructors = checkPrimaryConstructors;
            this.includeObject = includeObject;
            this.recurseIntoPackage = recurseIntoPackage;
            this.includeIntoOutput = includeIntoOutput;
        }

        public Configuration filterOutput(@NotNull Predicate<NamespaceDescriptor> includeIntoOutput) {
            return new Configuration(checkPrimaryConstructors, includeObject, recurseIntoPackage, includeIntoOutput);
        }

        public Configuration filterRecusion(@NotNull Predicate<FqNameUnsafe> recurseIntoPackage) {
            return new Configuration(checkPrimaryConstructors, includeObject, recurseIntoPackage, includeIntoOutput);
        }

        public Configuration checkPrimaryConstructors(boolean checkPrimaryConstructors) {
            return new Configuration(checkPrimaryConstructors, includeObject, recurseIntoPackage, includeIntoOutput);
        }
    }

    private final Configuration conf;
    private final DeferredAssertions deferred = new DeferredAssertions();

    private NamespaceComparator(@NotNull Configuration conf) {
        this.conf = conf;
    }

    private String doCompareNamespaces(@NotNull NamespaceDescriptor expectedNamespace, @NotNull NamespaceDescriptor actualNamespace) {
        StringBuilder sb = new StringBuilder();
        deferred.assertEquals(expectedNamespace.getName(), actualNamespace.getName());

        sb.append("namespace " + expectedNamespace.getName() + "\n\n");

        //deferred.assertTrue("namespace " + expectedNamespace.getName() + " is empty", !expectedNamespace.getMemberScope().getAllDescriptors().isEmpty());

        Set<Name> classifierNames = Sets.newHashSet();
        Set<Name> propertyNames = Sets.newHashSet();
        Set<Name> functionNames = Sets.newHashSet();
        Set<Name> objectNames = Sets.newHashSet();

        for (DeclarationDescriptor ad : expectedNamespace.getMemberScope().getAllDescriptors()) {
            if (ad instanceof ClassifierDescriptor) {
                classifierNames.add(ad.getName());
            }
            else if (ad instanceof PropertyDescriptor) {
                propertyNames.add(ad.getName());
            }
            else if (ad instanceof FunctionDescriptor) {
                functionNames.add(ad.getName());
            }
            else if (ad instanceof NamespaceDescriptor) {
                if (conf.recurseIntoPackage.apply(DescriptorUtils.getFQName(ad))) {
                    NamespaceDescriptor namespaceDescriptorA = (NamespaceDescriptor) ad;
                    NamespaceDescriptor namespaceDescriptorB = actualNamespace.getMemberScope().getNamespace(namespaceDescriptorA.getName());
                    //deferred.assertNotNull("Namespace not found: " + namespaceDescriptorA.getQualifiedName(), namespaceDescriptorB);
                    if (namespaceDescriptorB == null) {
                        System.err.println("Namespace not found: " + namespaceDescriptorA.getQualifiedName());
                    }
                    else {
                        String comparison = doCompareNamespaces(namespaceDescriptorA, namespaceDescriptorB);
                        if (conf.includeIntoOutput.apply(namespaceDescriptorA)) {
                            sb.append("// <namespace name=\"" + namespaceDescriptorA.getName() + "\">\n");
                            sb.append(comparison);
                            sb.append("// </namespace name=\"" + namespaceDescriptorA.getName() + "\">\n");
                        }
                    }
                }
            }
            else {
                throw new AssertionError("unknown member: " + ad);
            }
        }

        for (ClassDescriptor objectDescriptor : expectedNamespace.getMemberScope().getObjectDescriptors()) {
            objectNames.add(objectDescriptor.getName());
        }

        for (Name name : sorted(classifierNames)) {
            ClassifierDescriptor ca = expectedNamespace.getMemberScope().getClassifier(name);
            ClassifierDescriptor cb = actualNamespace.getMemberScope().getClassifier(name);
            deferred.assertTrue("Classifier not found in " + expectedNamespace + ": " + name, ca != null);
            deferred.assertTrue("Classifier not found in " + actualNamespace + ": " + name, cb != null);
            if (ca != null && cb != null) {
                compareClassifiers(ca, cb, sb);
            }
        }

        for (Name name : sorted(objectNames)) {
            ClassifierDescriptor ca = expectedNamespace.getMemberScope().getObjectDescriptor(name);
            ClassifierDescriptor cb = actualNamespace.getMemberScope().getObjectDescriptor(name);
            deferred.assertTrue("Object not found in " + expectedNamespace + ": " + name, ca != null);
            deferred.assertTrue("Object not found in " + actualNamespace + ": " + name, cb != null);
            if (ca != null && cb != null) {
                compareClassifiers(ca, cb, sb);
            }
        }

        for (Name name : sorted(propertyNames)) {
            Collection<VariableDescriptor> pa = expectedNamespace.getMemberScope().getProperties(name);
            Collection<VariableDescriptor> pb = actualNamespace.getMemberScope().getProperties(name);
            compareDeclarationSets("Properties in package " + expectedNamespace, pa, pb, sb);

            deferred.assertTrue(actualNamespace.getMemberScope().getFunctions(Name.identifier(PropertyCodegen.getterName(name))).isEmpty());
            deferred.assertTrue(actualNamespace.getMemberScope().getFunctions(Name.identifier(PropertyCodegen.setterName(name))).isEmpty());
        }

        for (Name name : sorted(functionNames)) {
            Collection<FunctionDescriptor> fa = expectedNamespace.getMemberScope().getFunctions(name);
            Collection<FunctionDescriptor> fb = actualNamespace.getMemberScope().getFunctions(name);
            compareDeclarationSets("Functions in package " + expectedNamespace, fa, fb, sb);
        }

        return sb.toString();
    }

    private void compareDeclarationSets(String message,
            Collection<? extends DeclarationDescriptor> a,
            Collection<? extends DeclarationDescriptor> b,
            @NotNull StringBuilder sb) {
        String at = serializedDeclarationSets(a);
        String bt = serializedDeclarationSets(b);
        deferred.assertEquals(message, at, bt);
        sb.append(at);
    }

    private String serializedDeclarationSets(Collection<? extends DeclarationDescriptor> ds) {
        List<String> strings = new ArrayList<String>();
        for (DeclarationDescriptor d : ds) {
            StringBuilder sb = new StringBuilder();
            new Serializer(conf.checkPrimaryConstructors, sb).serialize(d);
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
            // Serializers put "!" in front of the name in order to facilitate faster sorting of members
            int i = s.indexOf("!");
            if (i < 0) {
                throw new IllegalStateException("No name mark in " + s);
            }
            String substring = s.substring(i + 1);
            return substring;
        }

        @Override
        public int compare(@NotNull String a, @NotNull String b) {
            return normalize(a).compareTo(normalize(b));
        }
    }

    private void compareClassifiers(@NotNull ClassifierDescriptor a, @NotNull ClassifierDescriptor b, @NotNull StringBuilder sb) {
        StringBuilder sba = new StringBuilder();
        StringBuilder sbb = new StringBuilder();

        new FullContentSerialier(conf.checkPrimaryConstructors, sba).serialize((ClassDescriptor) a);
        new FullContentSerialier(conf.checkPrimaryConstructors, sbb).serialize((ClassDescriptor) b);

        String as = sba.toString();
        String bs = sbb.toString();

        deferred.assertEquals(as, bs);
        sb.append(as);
    }


    private static class MethodCache {
        // Argument type -> method
        private final Map<Class<?>, Method> methodCache = Maps.newHashMap();
        private final Class<? extends Serializer> serializerClass;

        private MethodCache(Class<? extends Serializer> aClass) {
            serializerClass = aClass;
        }

        @NotNull
        public Class<? extends Serializer> getSerializerClass() {
            return serializerClass;
        }

        private void buildMethodCache() {
            if (!methodCache.isEmpty()) return;
            for (Method method : serializerClass.getMethods()) {
                if (!method.getName().equals("serialize")) {
                    continue;
                }
                if (method.getParameterTypes().length != 1) {
                    continue;
                }
                if (method.getParameterTypes()[0].equals(Object.class)) {
                    continue;
                }
                method.setAccessible(true);
                methodCache.put(method.getParameterTypes()[0], method);
            }
        }

        @NotNull
        public Method getMethodToSerialize(Object o) {
            if (o == null) {
                throw new IllegalStateException("won't serialize null");
            }

            buildMethodCache();
            Method method = methodCache.get(o.getClass());
            if (method != null) {
                return method;
            }
            for (Map.Entry<Class<?>, Method> entry : methodCache.entrySet()) {
                Class<?> parameterType = entry.getKey();
                Method serializeMethod = entry.getValue();
                if (parameterType.isInstance(o)) {
                    methodCache.put(o.getClass(), serializeMethod);
                    return serializeMethod;
                }
            }
            throw new IllegalStateException("don't know how to serialize " + o + " (of " + o.getClass() + ")");
        }
    }


    private static class Serializer {

        private static final MethodCache SERIALIZER_METHOD_CACHE = new MethodCache(Serializer.class);

        protected final boolean checkPrimaryConstructors;
        protected final StringBuilder sb;


        public Serializer(boolean checkPrimaryConstructors, StringBuilder sb) {
            this.checkPrimaryConstructors = checkPrimaryConstructors;
            this.sb = sb;
        }

        protected MethodCache doGetMethodCache() {
            return SERIALIZER_METHOD_CACHE;
        }

        private MethodCache getMethodCache() {
            MethodCache methodCache = doGetMethodCache();
            if (methodCache.getSerializerClass() != this.getClass()) {
                throw new IllegalStateException("No method cache for class " + this.getClass() + ". Please, override doGetMethodCache()");
            }
            return methodCache;
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
                case ENUM_CLASS:
                    sb.append("enum class");
                    break;
                case ENUM_ENTRY:
                    sb.append("enum entry");
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

        // We want to skip @ExpectLoadErrors annotations when comparing
        private static List<AnnotationDescriptor> filterAnnotations(List<AnnotationDescriptor> annotations) {
            return ContainerUtil.filter(annotations, new Condition<AnnotationDescriptor>() {
                @Override
                public boolean value(AnnotationDescriptor annotation) {
                    ClassDescriptor annotationClass = (ClassDescriptor) annotation.getType().getConstructor().getDeclarationDescriptor();
                    assert annotationClass != null;

                    return !DescriptorUtils.getFQName(annotationClass).getFqName().equals(ExpectedLoadErrorsUtil.ANNOTATION_CLASS_NAME);
                }
            });
        }

        public void serialize(FunctionDescriptor fun) {
            serialize(fun.getVisibility());
            sb.append(" ");
            serialize(fun.getModality());
            sb.append(" ");

            List<AnnotationDescriptor> annotations = filterAnnotations(fun.getAnnotations());
            if (!annotations.isEmpty()) {
                new Serializer(checkPrimaryConstructors, sb).serializeSeparated(annotations, " ");
                sb.append(" ");
            }

            if (!fun.getOverriddenDescriptors().isEmpty()) {
                sb.append("override /*").append(fun.getOverriddenDescriptors().size()).append("*/ ");
            }

            if (fun.getKind() != CallableMemberDescriptor.Kind.DECLARATION) {
                sb.append("/*");
                new Serializer(checkPrimaryConstructors, sb).serialize(fun.getKind());
                sb.append("*/ ");
            }

            if (fun instanceof ConstructorDescriptor) {
                if (((ConstructorDescriptor) fun).isPrimary() && checkPrimaryConstructors) {
                    sb.append("/*primary constructor*/ ");
                }
                else {
                    sb.append("/*constructor*/ ");
                }
            }
            sb.append("fun ");
            if (!fun.getTypeParameters().isEmpty()) {
                sb.append("<");
                new Serializer(checkPrimaryConstructors, sb).serializeCommaSeparated(fun.getTypeParameters());
                sb.append(">");
            }

            sb.append("!");
            serializeReceiver(fun);
            sb.append(fun.getName());

            sb.append("(");
            new TypeSerializer(sb).serializeCommaSeparated(fun.getValueParameters());
            sb.append("): ");
            JetType returnType = fun.getReturnType();
            if (returnType == null) {
                throw new IllegalStateException("No return type for " + fun);
            }
            else {
                new TypeSerializer(sb).serialize(returnType);
            }
        }

        private void serializeReceiver(CallableDescriptor fun) {
            if (fun.getReceiverParameter() != null) {
                new TypeSerializer(sb).serialize(fun.getReceiverParameter());
                sb.append(".");
            }
        }

        public void serialize(ReceiverParameterDescriptor receiverParameterDescriptor) {
            serialize(receiverParameterDescriptor.getType());
        }

        private void serialize(@NotNull PropertyAccessorDescriptor accessor) {
            if (accessor.getCorrespondingProperty().getVisibility() == accessor.getVisibility()) {
                // don't serialize if visibility is the same
                return;
            }
            sb.append(" ");
            sb.append(accessor.getVisibility());
            if (accessor instanceof PropertyGetterDescriptor) {
                sb.append(" get");
            }
            else if (accessor instanceof PropertySetterDescriptor) {
                sb.append(" set");
            }
            else {
                throw new IllegalArgumentException("neither getter nor setter");
            }
        }

        public void serialize(PropertyDescriptor prop) {
            serialize(prop.getVisibility());
            sb.append(" ");
            serialize(prop.getModality());
            sb.append(" ");

            List<AnnotationDescriptor> annotations = filterAnnotations(prop.getAnnotations());
            if (!annotations.isEmpty()) {
                new Serializer(checkPrimaryConstructors, sb).serializeSeparated(annotations, " ");
                sb.append(" ");
            }

            if (!prop.getOverriddenDescriptors().isEmpty()) {
                sb.append("override /*").append(prop.getOverriddenDescriptors().size()).append("*/ ");
            }

            if (prop.getKind() != CallableMemberDescriptor.Kind.DECLARATION) {
                sb.append("/*");
                new Serializer(checkPrimaryConstructors, sb).serialize(prop.getKind());
                sb.append("*/ ");
            }

            if (prop.isVar()) {
                sb.append("var ");
            }
            else {
                sb.append("val ");
            }
            if (!prop.getTypeParameters().isEmpty()) {
                sb.append(" <");
                new Serializer(checkPrimaryConstructors, sb).serializeCommaSeparated(prop.getTypeParameters());
                sb.append("> ");
            }
            sb.append("!");
            ReceiverParameterDescriptor receiverParameter = prop.getReceiverParameter();
            if (receiverParameter != null) {
                new TypeSerializer(sb).serialize(receiverParameter.getType());
                sb.append(".");
            }
            sb.append(prop.getName());
            sb.append(": ");
            new TypeSerializer(sb).serialize(prop.getType());

            for (PropertyAccessorDescriptor accessor : prop.getAccessors()) {
                serialize(accessor);
            }
        }

        public void serialize(ValueParameterDescriptor valueParameter) {
            sb.append("/*");
            sb.append(valueParameter.getIndex());
            sb.append("*/ ");
            if (valueParameter.getVarargElementType() != null) {
                sb.append("vararg ");
            }
            sb.append("!");
            sb.append(valueParameter.getName());
            sb.append(": ");
            if (valueParameter.getVarargElementType() != null) {
                new TypeSerializer(sb).serialize(valueParameter.getVarargElementType());
                sb.append(" /*");
                new TypeSerializer(sb).serialize(valueParameter.getType());
                sb.append("*/");
            }
            else {
                new TypeSerializer(sb).serialize(valueParameter.getType());
            }
            if (valueParameter.hasDefaultValue()) {
                sb.append(" = ?");
            }
        }

        public void serialize(Variance variance) {
            if (variance == Variance.INVARIANT) {

            }
            else {
                sb.append(variance);
                sb.append(' ');
            }
        }

        public void serialize(Modality modality) {
            sb.append(modality.name().toLowerCase());
        }

        public void serialize(Visibility visibility) {
            sb.append(visibility);
        }

        public void serialize(CallableMemberDescriptor.Kind kind) {
            sb.append(kind.name().toLowerCase());
        }

        public void serialize(AnnotationDescriptor annotation) {
            new TypeSerializer(sb).serialize(annotation.getType());
            sb.append("(");
            serializeCommaSeparated(DescriptorUtils.getSortedValueArguments(annotation));
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

        public void serialize(Object o) {
            Method method = getMethodCache().getMethodToSerialize(o);
            invoke(method, this, o);
        }

        public void serialize(String s) {
            sb.append(s);
        }

        public void serialize(CompileTimeConstant s) {
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

        private static final MethodCache TYPE_SERIALIZER_METHOD_CACHE = new MethodCache(TypeSerializer.class);

        public TypeSerializer(StringBuilder sb) {
            super(false, sb);
        }

        @Override
        protected MethodCache doGetMethodCache() {
            return TYPE_SERIALIZER_METHOD_CACHE;
        }

        @Override
        public void serialize(TypeParameterDescriptor param) {
            sb.append(param.getName());
        }

        public void serialize(@NotNull JetType type) {
            if (ErrorUtils.isErrorType(type)) {
                sb.append(type);
                return;
            }
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

        private static final MethodCache NAMESPACE_PREFIX_SERIALIZER_METHOD_CACHE = new MethodCache(NamespacePrefixSerializer.class);

        private NamespacePrefixSerializer(boolean checkPrimaryConstructors, StringBuilder sb) {
            super(checkPrimaryConstructors, sb);
        }

        @Override
        protected MethodCache doGetMethodCache() {
            return NAMESPACE_PREFIX_SERIALIZER_METHOD_CACHE;
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

    private static final MethodCache FULL_CONTENT_SERIALIZER_METHOD_CACHE = new MethodCache(FullContentSerialier.class);
    private class FullContentSerialier extends Serializer {

        private FullContentSerialier(boolean checkPrimaryConstructors, StringBuilder sb) {
            super(checkPrimaryConstructors, sb);
        }

        @Override
        protected MethodCache doGetMethodCache() {
            return FULL_CONTENT_SERIALIZER_METHOD_CACHE;
        }

        public void serialize(ClassDescriptor klass) {

            if (!klass.getAnnotations().isEmpty()) {
                new Serializer(checkPrimaryConstructors, sb).serializeSeparated(klass.getAnnotations(), " ");
                sb.append(" ");
            }
            serialize(klass.getVisibility());
            sb.append(" ");
            serialize(klass.getModality());
            sb.append(" ");

            serialize(klass.getKind());
            sb.append(" ");

            sb.append("!");
            new Serializer(checkPrimaryConstructors, sb).serialize(klass);

            if (!klass.getTypeConstructor().getParameters().isEmpty()) {
                sb.append("<");
                serializeCommaSeparated(klass.getTypeConstructor().getParameters());
                sb.append(">");
            }

            if (KotlinBuiltIns.getInstance().getNothing() != klass && !klass.getTypeConstructor().getSupertypes().isEmpty()) {
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
                new Serializer(checkPrimaryConstructors, constructorSb).serialize(constructor);
                memberStrings.add(constructorSb.toString());
            }

            JetScope memberScope = klass.getMemberScope(typeArguments);
            for (DeclarationDescriptor member : memberScope.getAllDescriptors()) {
                if (!conf.includeObject) {
                    if (member.getName().getName().matches("equals|hashCode|finalize|wait|notify(All)?|toString|clone|getClass")) {
                        continue;
                    }
                }
                StringBuilder memberSb = new StringBuilder();
                new FullContentSerialier(checkPrimaryConstructors, memberSb).serialize(member);
                memberStrings.add(memberSb.toString());
            }

            for (DeclarationDescriptor object : memberScope.getObjectDescriptors()) {
                StringBuilder objectSb = new StringBuilder();
                new FullContentSerialier(checkPrimaryConstructors, objectSb).serialize(object);
                memberStrings.add(objectSb.toString());
            }

            Collections.sort(memberStrings, new MemberComparator());

            for (String memberString : memberStrings) {
                sb.append(indent(memberString));
            }

            if (klass.getClassObjectDescriptor() != null) {
                StringBuilder sbForClassObject = new StringBuilder();
                new ClassObjectSerializer(sbForClassObject).serialize(klass.getClassObjectDescriptor());
                sb.append(indent(sbForClassObject.toString()));
            }

            sb.append("}\n");
        }
    }

    private static final MethodCache CLASS_OBJECT_SERIALIZER_METHOD_CACHE = new MethodCache(ClassObjectSerializer.class);
    private class ClassObjectSerializer extends FullContentSerialier {

        private ClassObjectSerializer(StringBuilder sb) {
            super(false, sb);
        }

        @Override
        protected MethodCache doGetMethodCache() {
            return CLASS_OBJECT_SERIALIZER_METHOD_CACHE;
        }

        @Override
        public void serialize(ClassKind kind) {
            assert kind == ClassKind.CLASS_OBJECT : "Must be called for class objects only";
            sb.append("class object");
        }
    }


    private static String indent(String string) {
        // This method gets called a lot, so the performance is critical
        // That's why the hand-written code

        String indent = "    ";
        StringBuilder r = new StringBuilder(string.length());
        r.append(indent);
        boolean lastCharIsNewLine = false;
        for (int i = 0; i < string.length(); i++) {
             char c = string.charAt(i);
            r.append(c);
            if (c == '\n') {
                if (i != string.length() - 1) {
                    r.append(indent);
                }
                else {
                    lastCharIsNewLine = true;
                }
            }
        }
        if (!lastCharIsNewLine) {
            r.append("\n");
        }
        return r.toString();
    }

    private static <T extends Comparable<T>> List<T> sorted(Collection<T> items) {
        List<T> r = new ArrayList<T>(items);
        Collections.sort(r);
        return r;
    }

    private static class DeferredAssertions {
        private final List<AssertionError> assertionFailures = Lists.newArrayList();

        private void assertTrue(String message, Boolean b) {
            try {
                Assert.assertTrue(message, b);
            }
            catch (AssertionError e) {
                assertionFailures.add(e);
            }
        }

        private void assertTrue(Boolean b) {
            try {
                Assert.assertTrue(b);
            }
            catch (AssertionError e) {
                assertionFailures.add(e);
            }
        }

        private void assertNotNull(Object a) {
            try {
                Assert.assertNotNull(a);
            }
            catch (AssertionError e) {
                assertionFailures.add(e);
            }
        }

        private void assertEquals(Object a, Object b) {
            try {
                Assert.assertEquals(a, b);
            }
            catch (AssertionError e) {
                assertionFailures.add(e);
            }
        }

        private void assertEquals(String message, Object a, Object b) {
            try {
                Assert.assertEquals(message, a, b);
            }
            catch (AssertionError e) {
                assertionFailures.add(e);
            }
        }

        public void throwFailures() {
            StringBuilder expected = new StringBuilder();
            StringBuilder actual = new StringBuilder();
            for (AssertionError failure : assertionFailures) {
                if (failure instanceof ComparisonFailure) {
                    ComparisonFailure comparisonFailure = (ComparisonFailure) failure;
                    expected.append(comparisonFailure.getExpected());
                    actual.append(comparisonFailure.getActual());
                }
                else {
                    throw failure;
                }
            }
            Assert.assertEquals(expected.toString(), actual.toString());
        }

        public boolean failed() {
            return !assertionFailures.isEmpty();
        }
    }
}
