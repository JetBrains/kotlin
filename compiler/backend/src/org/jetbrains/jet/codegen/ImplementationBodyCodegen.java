/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen;

import com.google.common.collect.Lists;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.util.ArrayUtil;
import kotlin.Function0;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.backend.common.CodegenUtil;
import org.jetbrains.jet.backend.common.DataClassMethodGenerator;
import org.jetbrains.jet.codegen.binding.MutableClosure;
import org.jetbrains.jet.codegen.bridges.BridgesPackage;
import org.jetbrains.jet.codegen.context.ClassContext;
import org.jetbrains.jet.codegen.context.ConstructorContext;
import org.jetbrains.jet.codegen.context.FieldOwnerContext;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.signature.BothSignatureWriter;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.descriptors.serialization.BitEncoding;
import org.jetbrains.jet.descriptors.serialization.ClassData;
import org.jetbrains.jet.descriptors.serialization.DescriptorSerializer;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DeclarationResolver;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.bindingContextUtil.BindingContextUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.CallResolverUtil;
import org.jetbrains.jet.lang.resolve.calls.model.DefaultValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.ExpressionValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.VarargValueArgument;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.jet.lang.resolve.java.jvmSignature.JvmClassSignature;
import org.jetbrains.jet.lang.resolve.java.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.jet.lang.resolve.java.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.jet.lang.resolve.java.jvmSignature.JvmMethodSignature;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.*;

import static org.jetbrains.jet.codegen.AsmUtil.*;
import static org.jetbrains.jet.codegen.JvmCodegenUtil.*;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.*;
import static org.jetbrains.jet.descriptors.serialization.NameSerializationUtil.createNameResolver;
import static org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils.descriptorToDeclaration;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.*;
import static org.jetbrains.jet.lang.resolve.bindingContextUtil.BindingContextUtilPackage.getResolvedCallWithAssert;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.*;
import static org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.KotlinSyntheticClass;
import static org.jetbrains.jet.lang.resolve.java.diagnostics.DiagnosticsPackage.DelegationToTraitImpl;
import static org.jetbrains.jet.lang.resolve.java.diagnostics.DiagnosticsPackage.OtherOrigin;
import static org.jetbrains.jet.lang.resolve.java.diagnostics.JvmDeclarationOrigin.NO_ORIGIN;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class ImplementationBodyCodegen extends ClassBodyCodegen {
    private static final String VALUES = "$VALUES";
    private JetDelegatorToSuperCall superCall;
    private Type superClassAsmType;
    @Nullable // null means java/lang/Object
    private JetType superClassType;
    private final Type classAsmType;

    private List<PropertyAndDefaultValue> classObjectPropertiesToCopy;

    public ImplementationBodyCodegen(
            @NotNull JetClassOrObject aClass,
            @NotNull ClassContext context,
            @NotNull ClassBuilder v,
            @NotNull GenerationState state,
            @Nullable MemberCodegen<?> parentCodegen
    ) {
        super(aClass, context, v, state, parentCodegen);
        this.classAsmType = typeMapper.mapClass(descriptor);
    }

    @Override
    protected void generateDeclaration() {
        getSuperClass();

        JvmClassSignature signature = signature();

        boolean isAbstract = false;
        boolean isInterface = false;
        boolean isFinal = false;
        boolean isStatic;
        boolean isAnnotation = false;
        boolean isEnum = false;

        if (myClass instanceof JetClass) {
            JetClass jetClass = (JetClass) myClass;
            if (jetClass.hasModifier(JetTokens.ABSTRACT_KEYWORD)) {
                isAbstract = true;
            }
            if (jetClass.isTrait()) {
                isAbstract = true;
                isInterface = true;
            }
            else if (jetClass.isAnnotation()) {
                isAbstract = true;
                isInterface = true;
                isAnnotation = true;
                signature.getInterfaces().add("java/lang/annotation/Annotation");
            }
            else if (jetClass.isEnum()) {
                isAbstract = hasAbstractMembers(descriptor);
                isEnum = true;
            }

            if (descriptor.getKind() == ClassKind.OBJECT || descriptor.getKind() == ClassKind.CLASS_OBJECT) {
                isFinal = true;
            }

            if (!jetClass.hasModifier(JetTokens.OPEN_KEYWORD) && !isAbstract) {
                isFinal = true;
            }
            isStatic = !jetClass.isInner();
        }
        else {
            isStatic = myClass.getParent() instanceof JetClassObject;
            isFinal = true;
        }

        int access = 0;

        if (state.getClassBuilderMode() == ClassBuilderMode.LIGHT_CLASSES && !DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            // ClassBuilderMode.LIGHT_CLASSES means we are generating light classes & looking at a nested or inner class
            // Light class generation is implemented so that Cls-classes only read bare code of classes,
            // without knowing whether these classes are inner or not (see ClassStubBuilder.EMPTY_STRATEGY)
            // Thus we must write full accessibility flags on inner classes in this mode
            access |= getVisibilityAccessFlag(descriptor);
            // Same for STATIC
            if (isStatic) {
                access |= ACC_STATIC;
            }
        }
        else {
            access |= getVisibilityAccessFlagForClass(descriptor);
        }
        if (isAbstract) {
            access |= ACC_ABSTRACT;
        }
        if (isInterface) {
            access |= ACC_INTERFACE; // ACC_SUPER
        }
        else {
            access |= ACC_SUPER;
        }
        if (isFinal) {
            access |= ACC_FINAL;
        }
        if (isAnnotation) {
            access |= ACC_ANNOTATION;
        }
        if (KotlinBuiltIns.getInstance().isDeprecated(descriptor)) {
            access |= ACC_DEPRECATED;
        }
        if (isEnum) {
            for (JetDeclaration declaration : myClass.getDeclarations()) {
                if (declaration instanceof JetEnumEntry) {
                    if (enumEntryNeedSubclass(state.getBindingContext(), (JetEnumEntry) declaration)) {
                        access &= ~ACC_FINAL;
                    }
                }
            }
            access |= ACC_ENUM;
        }
        List<String> interfaces = signature.getInterfaces();
        v.defineClass(myClass, V1_6,
                      access,
                      signature.getName(),
                      signature.getJavaGenericSignature(),
                      signature.getSuperclassName(),
                      ArrayUtil.toStringArray(interfaces)
        );
        v.visitSource(myClass.getContainingFile().getName(), null);

        writeEnclosingMethod();

        writeOuterClasses();

        writeInnerClasses();

        AnnotationCodegen.forClass(v.getVisitor(), typeMapper).genAnnotations(descriptor, null);

        generateReflectionObjectFieldIfNeeded();
    }

    @Override
    protected void generateKotlinAnnotation() {
        if (isAnonymousObject(descriptor)) {
            writeKotlinSyntheticClassAnnotation(v, KotlinSyntheticClass.Kind.ANONYMOUS_OBJECT);
            return;
        }

        if (!isTopLevelOrInnerClass(descriptor)) {
            // LOCAL_CLASS is also written to inner classes of local classes
            writeKotlinSyntheticClassAnnotation(v, KotlinSyntheticClass.Kind.LOCAL_CLASS);
            return;
        }

        if (state.getClassBuilderMode() != ClassBuilderMode.FULL) return;

        DescriptorSerializer serializer = new DescriptorSerializer(new JavaSerializerExtension(v.getSerializationBindings()));

        ProtoBuf.Class classProto = serializer.classProto(descriptor).build();

        ClassData data = new ClassData(createNameResolver(serializer.getNameTable()), classProto);

        AnnotationVisitor av = v.getVisitor().visitAnnotation(asmDescByFqNameWithoutInnerClasses(JvmAnnotationNames.KOTLIN_CLASS), true);
        //noinspection ConstantConditions
        av.visit(JvmAnnotationNames.ABI_VERSION_FIELD_NAME, JvmAbi.VERSION);
        AnnotationVisitor array = av.visitArray(JvmAnnotationNames.DATA_FIELD_NAME);
        for (String string : BitEncoding.encodeBytes(data.toBytes())) {
            array.visit(null, string);
        }
        array.visitEnd();
        av.visitEnd();
    }

    private void writeEnclosingMethod() {
        //JVMS7: A class must have an EnclosingMethod attribute if and only if it is a local class or an anonymous class.
        DeclarationDescriptor parentDescriptor = descriptor.getContainingDeclaration();

        boolean isObjectLiteral = DescriptorUtils.isAnonymousObject(descriptor);

        boolean isLocalOrAnonymousClass = isObjectLiteral ||
                                          !(parentDescriptor instanceof PackageFragmentDescriptor || parentDescriptor instanceof ClassDescriptor);
        // Do not emit enclosing method in "light-classes mode" since currently we generate local light classes as if they're top level
        if (isLocalOrAnonymousClass && state.getClassBuilderMode() != ClassBuilderMode.LIGHT_CLASSES) {
            writeOuterClassAndEnclosingMethod(descriptor, descriptor, typeMapper, v);
        }
    }

    private void writeInnerClasses() {
        Collection<ClassDescriptor> result = bindingContext.get(INNER_CLASSES, descriptor);
        if (result != null) {
            for (ClassDescriptor innerClass : result) {
                writeInnerClass(innerClass);
            }
        }
    }

    private void writeOuterClasses() {
        // JVMS7 (4.7.6): a nested class or interface member will have InnerClasses information
        // for each enclosing class and for each immediate member
        DeclarationDescriptor inner = descriptor;
        while (true) {
            if (inner == null || isTopLevelDeclaration(inner)) {
                break;
            }
            if (inner instanceof ClassDescriptor) {
                writeInnerClass((ClassDescriptor) inner);
            }
            inner = inner.getContainingDeclaration();
        }
    }

    private void writeInnerClass(@NotNull ClassDescriptor innerClass) {
        // TODO: proper access
        int innerClassAccess = getVisibilityAccessFlag(innerClass);
        if (innerClass.getModality() == Modality.FINAL) {
            innerClassAccess |= ACC_FINAL;
        }
        else if (innerClass.getModality() == Modality.ABSTRACT) {
            innerClassAccess |= ACC_ABSTRACT;
        }

        if (innerClass.getKind() == ClassKind.TRAIT) {
            innerClassAccess |= ACC_INTERFACE;
        }
        else if (innerClass.getKind() == ClassKind.ENUM_CLASS) {
            innerClassAccess |= ACC_ENUM;
        }

        if (!innerClass.isInner()) {
            innerClassAccess |= ACC_STATIC;
        }

        // TODO: cache internal names
        DeclarationDescriptor containing = innerClass.getContainingDeclaration();
        String outerClassInternalName = containing instanceof ClassDescriptor ? getInternalNameForImpl((ClassDescriptor) containing) : null;

        String innerClassInternalName;
        String innerName;

        if (isClassObject(innerClass)) {
            innerName = JvmAbi.CLASS_OBJECT_CLASS_NAME;
            innerClassInternalName = outerClassInternalName + JvmAbi.CLASS_OBJECT_SUFFIX;
        }
        else {
            innerName = innerClass.getName().isSpecial() ? null : innerClass.getName().asString();
            innerClassInternalName = getInternalNameForImpl(innerClass);
        }

        v.visitInnerClass(innerClassInternalName, outerClassInternalName, innerName, innerClassAccess);
    }

    @NotNull
    private String getInternalNameForImpl(@NotNull ClassDescriptor descriptor) {
        return typeMapper.mapClass(descriptor).getInternalName();
    }

    @NotNull
    private JvmClassSignature signature() {
        BothSignatureWriter sw = new BothSignatureWriter(BothSignatureWriter.Mode.CLASS);

        typeMapper.writeFormalTypeParameters(descriptor.getTypeConstructor().getParameters(), sw);

        sw.writeSuperclass();
        if (superClassType == null) {
            sw.writeClassBegin(superClassAsmType);
            sw.writeClassEnd();
        }
        else {
            typeMapper.mapSupertype(superClassType, sw);
        }
        sw.writeSuperclassEnd();

        List<JetType> interfaceSupertypes = Lists.newArrayList();
        boolean explicitKObject = false;

        for (JetDelegationSpecifier specifier : myClass.getDelegationSpecifiers()) {
            JetType superType = bindingContext.get(BindingContext.TYPE, specifier.getTypeReference());
            assert superType != null : "No supertype for class: " + myClass.getText();
            ClassifierDescriptor classifierDescriptor = superType.getConstructor().getDeclarationDescriptor();
            if (classifierDescriptor instanceof ClassDescriptor) {
                ClassDescriptor superClassDescriptor = (ClassDescriptor) classifierDescriptor;
                if (isInterface(superClassDescriptor)) {
                    interfaceSupertypes.add(superType);

                    if (JvmAbi.K_OBJECT.equalsTo(DescriptorUtils.getFqName(superClassDescriptor))) {
                        explicitKObject = true;
                    }
                }
            }
        }

        LinkedHashSet<String> superInterfaces = new LinkedHashSet<String>();
        if (!explicitKObject) {
            Type kObject = asmTypeByFqNameWithoutInnerClasses(JvmAbi.K_OBJECT);
            sw.writeInterface();
            sw.writeClassBegin(kObject);
            sw.writeClassEnd();
            sw.writeInterfaceEnd();
            superInterfaces.add(kObject.getInternalName());
        }

        for (JetType supertype : interfaceSupertypes) {
            sw.writeInterface();
            Type jvmName = typeMapper.mapSupertype(supertype, sw);
            sw.writeInterfaceEnd();
            superInterfaces.add(jvmName.getInternalName());
        }

        return new JvmClassSignature(classAsmType.getInternalName(), superClassAsmType.getInternalName(),
                                     new ArrayList<String>(superInterfaces),
                                     sw.makeJavaGenericSignature());
    }

    protected void getSuperClass() {
        superClassAsmType = AsmTypeConstants.OBJECT_TYPE;
        superClassType = null;

        List<JetDelegationSpecifier> delegationSpecifiers = myClass.getDelegationSpecifiers();

        if (myClass instanceof JetClass && ((JetClass) myClass).isTrait()) {
            return;
        }

        for (JetDelegationSpecifier specifier : delegationSpecifiers) {
            if (specifier instanceof JetDelegatorToSuperCall) {
                JetType superType = bindingContext.get(BindingContext.TYPE, specifier.getTypeReference());
                assert superType != null :
                        String.format("No type recorded for \n---\n%s\n---\n", JetPsiUtil.getElementTextWithContext(specifier));

                ClassifierDescriptor classifierDescriptor = superType.getConstructor().getDeclarationDescriptor();
                if (!(classifierDescriptor instanceof ClassDescriptor)) continue;

                ClassDescriptor superClassDescriptor = (ClassDescriptor) classifierDescriptor;
                if (!isInterface(superClassDescriptor)) {
                    superClassType = superType;
                    superClassAsmType = typeMapper.mapClass(superClassDescriptor);
                    superCall = (JetDelegatorToSuperCall) specifier;
                }
            }
        }

        if (superClassType == null) {
            if (descriptor.getKind() == ClassKind.ENUM_CLASS) {
                superClassType = KotlinBuiltIns.getInstance().getEnumType(descriptor.getDefaultType());
                superClassAsmType = typeMapper.mapType(superClassType);
            }
            if (descriptor.getKind() == ClassKind.ENUM_ENTRY) {
                superClassType = descriptor.getTypeConstructor().getSupertypes().iterator().next();
                superClassAsmType = typeMapper.mapType(superClassType);
            }
        }
    }

    @Override
    protected void generateSyntheticParts() {
        generatePropertyMetadataArrayFieldIfNeeded(classAsmType);

        generateFieldForSingleton();

        generateClassObjectBackingFieldCopies();

        DelegationFieldsInfo delegationFieldsInfo = getDelegationFieldsInfo(myClass.getDelegationSpecifiers());
        try {
            generatePrimaryConstructor(delegationFieldsInfo);
        }
        catch (CompilationException e) {
            throw e;
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (RuntimeException e) {
            throw new RuntimeException("Error generating primary constructor of class " + myClass.getName() + " with kind " + kind, e);
        }

        generateTraitMethods();

        generateDelegates(delegationFieldsInfo);

        generateSyntheticAccessors();

        generateEnumMethodsAndConstInitializers();

        generateFunctionsForDataClasses();

        generateBuiltinMethodStubs();

        generateToArray();

        genClosureFields(context.closure, v, typeMapper);
    }

    private void generateReflectionObjectFieldIfNeeded() {
        if (isAnnotationClass(descriptor)) {
            // There's a bug in JDK 6 and 7 that prevents us from generating a static field in an annotation class:
            // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6857918
            // TODO: make reflection work on annotation classes somehow
            return;
        }

        generateReflectionObjectField(state, classAsmType, v, method("kClassFromKotlin", K_CLASS_IMPL_TYPE, getType(Class.class)),
                                      JvmAbi.KOTLIN_CLASS_FIELD_NAME, createOrGetClInitCodegen().v);
    }

    private boolean isGenericToArrayPresent() {
        Collection<FunctionDescriptor> functions = descriptor.getDefaultType().getMemberScope().getFunctions(Name.identifier("toArray"));
        for (FunctionDescriptor function : functions) {
            if (CallResolverUtil.isOrOverridesSynthesized(function)) {
                continue;
            }

            if (function.getValueParameters().size() != 1 || function.getTypeParameters().size() != 1) {
                continue;
            }

            JetType arrayType = KotlinBuiltIns.getInstance().getArrayType(function.getTypeParameters().get(0).getDefaultType());
            JetType returnType = function.getReturnType();
            assert returnType != null : function.toString();
            JetType paramType = function.getValueParameters().get(0).getType();
            if (JetTypeChecker.DEFAULT.equalTypes(arrayType, returnType) && JetTypeChecker.DEFAULT.equalTypes(arrayType, paramType)) {
                return true;
            }
        }
        return false;

    }

    private void generateToArray() {
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        if (!isSubclass(descriptor, builtIns.getCollection())) return;

        int access = descriptor.getKind() == ClassKind.TRAIT ?
                     ACC_PUBLIC | ACC_ABSTRACT :
                     ACC_PUBLIC;
        if (JvmCodegenUtil.getDeclaredFunctionByRawSignature(descriptor, Name.identifier("toArray"), builtIns.getArray()) == null) {
            MethodVisitor mv = v.newMethod(NO_ORIGIN, access, "toArray", "()[Ljava/lang/Object;", null, null);

            if (descriptor.getKind() != ClassKind.TRAIT) {
                InstructionAdapter iv = new InstructionAdapter(mv);
                mv.visitCode();

                iv.load(0, classAsmType);
                iv.invokestatic("kotlin/jvm/internal/CollectionToArray", "toArray", "(Ljava/util/Collection;)[Ljava/lang/Object;", false);
                iv.areturn(Type.getObjectType("[Ljava/lang/Object;"));

                FunctionCodegen.endVisit(mv, "toArray", myClass);
            }
        }

        if (!isGenericToArrayPresent()) {
            MethodVisitor mv = v.newMethod(NO_ORIGIN, access, "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", null, null);

            if (descriptor.getKind() != ClassKind.TRAIT) {
                InstructionAdapter iv = new InstructionAdapter(mv);
                mv.visitCode();

                iv.load(0, classAsmType);
                iv.load(1, Type.getObjectType("[Ljava/lang/Object;"));

                iv.invokestatic("kotlin/jvm/internal/CollectionToArray", "toArray",
                                "(Ljava/util/Collection;[Ljava/lang/Object;)[Ljava/lang/Object;", false);
                iv.areturn(Type.getObjectType("[Ljava/lang/Object;"));

                FunctionCodegen.endVisit(mv, "toArray", myClass);
            }
        }
    }

    private class MethodStubGenerator {
        private final Set<String> generatedSignatures = new HashSet<String>();

        public void generate(
                @NotNull String name,
                @NotNull String desc,
                @NotNull ClassifierDescriptor returnedClassifier,
                @NotNull ClassifierDescriptor... valueParameterClassifiers
        ) {
            // avoid generating same signature twice
            if (!generatedSignatures.add(name + desc)) return;
            if (JvmCodegenUtil.getDeclaredFunctionByRawSignature(
                    descriptor, Name.identifier(name), returnedClassifier, valueParameterClassifiers) == null) {
                int access = descriptor.getKind() == ClassKind.TRAIT ?
                             ACC_PUBLIC | ACC_ABSTRACT :
                             ACC_PUBLIC;
                MethodVisitor mv = v.newMethod(NO_ORIGIN, access, name, desc, null, null);
                if (descriptor.getKind() != ClassKind.TRAIT) {
                    mv.visitCode();
                    genThrow(mv, "java/lang/UnsupportedOperationException", "Mutating immutable collection");
                    FunctionCodegen.endVisit(mv, "built-in stub for " + name + desc, null);
                }
            }
        }
    }

    private void generateBuiltinMethodStubs() {
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        MethodStubGenerator methodStubs = new MethodStubGenerator();
        if (isSubclass(descriptor, builtIns.getCollection())) {
            ClassifierDescriptor classifier = getSubstituteForTypeParameterOf(builtIns.getCollection(), 0);

            methodStubs.generate("add", "(Ljava/lang/Object;)Z", builtIns.getBoolean(), classifier);
            methodStubs.generate("remove", "(Ljava/lang/Object;)Z", builtIns.getBoolean(), builtIns.getAny());
            methodStubs.generate("addAll", "(Ljava/util/Collection;)Z", builtIns.getBoolean(), builtIns.getCollection());
            methodStubs.generate("removeAll", "(Ljava/util/Collection;)Z", builtIns.getBoolean(), builtIns.getCollection());
            methodStubs.generate("retainAll", "(Ljava/util/Collection;)Z", builtIns.getBoolean(), builtIns.getCollection());
            methodStubs.generate("clear", "()V", builtIns.getUnit());
        }

        if (isSubclass(descriptor, builtIns.getList())) {
            ClassifierDescriptor classifier = getSubstituteForTypeParameterOf(builtIns.getList(), 0);

            methodStubs.generate("set", "(ILjava/lang/Object;)Ljava/lang/Object;", classifier, builtIns.getInt(), classifier);
            methodStubs.generate("add", "(ILjava/lang/Object;)V", builtIns.getUnit(), builtIns.getInt(), classifier);
            methodStubs.generate("remove", "(I)Ljava/lang/Object;", classifier, builtIns.getInt());
        }

        if (isSubclass(descriptor, builtIns.getMap())) {
            ClassifierDescriptor keyClassifier = getSubstituteForTypeParameterOf(builtIns.getMap(), 0);
            ClassifierDescriptor valueClassifier = getSubstituteForTypeParameterOf(builtIns.getMap(), 1);

            methodStubs.generate("put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", valueClassifier, keyClassifier,
                                 valueClassifier);
            methodStubs.generate("remove", "(Ljava/lang/Object;)Ljava/lang/Object;", valueClassifier, builtIns.getAny());
            methodStubs.generate("putAll", "(Ljava/util/Map;)V", builtIns.getUnit(), builtIns.getMap());
            methodStubs.generate("clear", "()V", builtIns.getUnit());
        }

        if (isSubclass(descriptor, builtIns.getMapEntry())) {
            ClassifierDescriptor valueClassifier = getSubstituteForTypeParameterOf(builtIns.getMapEntry(), 1);

            methodStubs.generate("setValue", "(Ljava/lang/Object;)Ljava/lang/Object;", valueClassifier, valueClassifier);
        }

        if (isSubclass(descriptor, builtIns.getIterator())) {
            methodStubs.generate("remove", "()V", builtIns.getUnit());
        }
    }

    @NotNull
    private ClassifierDescriptor getSubstituteForTypeParameterOf(@NotNull ClassDescriptor trait, int index) {
        TypeParameterDescriptor listTypeParameter = trait.getTypeConstructor().getParameters().get(index);
        TypeSubstitutor deepSubstitutor = SubstitutionUtils.buildDeepSubstitutor(descriptor.getDefaultType());
        TypeProjection substitute = deepSubstitutor.substitute(new TypeProjectionImpl(listTypeParameter.getDefaultType()));
        assert substitute != null : "Couldn't substitute: " + descriptor;
        ClassifierDescriptor classifier = substitute.getType().getConstructor().getDeclarationDescriptor();
        assert classifier != null : "No classifier: " + substitute.getType();
        return classifier;
    }

    private void generateFunctionsForDataClasses() {
        if (!KotlinBuiltIns.getInstance().isData(descriptor)) return;

        new DataClassMethodGeneratorImpl(myClass, bindingContext).generate();
    }

    private class DataClassMethodGeneratorImpl extends DataClassMethodGenerator {
        DataClassMethodGeneratorImpl(
                JetClassOrObject klass,
                BindingContext bindingContext
        ) {
            super(klass, bindingContext);
        }

        @Override
        public void generateEqualsMethod(@NotNull List<PropertyDescriptor> properties) {
            FunctionDescriptor equalsFunction = CodegenUtil.getDeclaredFunctionByRawSignature(
                    descriptor, Name.identifier(CodegenUtil.EQUALS_METHOD_NAME),
                    KotlinBuiltIns.getInstance().getBoolean(),
                    KotlinBuiltIns.getInstance().getAny()
            );
            MethodContext context = ImplementationBodyCodegen.this.context.intoFunction(equalsFunction);
            MethodVisitor mv = v.newMethod(OtherOrigin(equalsFunction), ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
            InstructionAdapter iv = new InstructionAdapter(mv);

            mv.visitCode();
            Label eq = new Label();
            Label ne = new Label();

            iv.load(0, OBJECT_TYPE);
            iv.load(1, AsmTypeConstants.OBJECT_TYPE);
            iv.ifacmpeq(eq);

            iv.load(1, AsmTypeConstants.OBJECT_TYPE);
            iv.instanceOf(classAsmType);
            iv.ifeq(ne);

            iv.load(1, AsmTypeConstants.OBJECT_TYPE);
            iv.checkcast(classAsmType);
            iv.store(2, AsmTypeConstants.OBJECT_TYPE);

            for (PropertyDescriptor propertyDescriptor : properties) {
                Type asmType = typeMapper.mapType(propertyDescriptor);

                genPropertyOnStack(iv, context, propertyDescriptor, 0);
                genPropertyOnStack(iv, context, propertyDescriptor, 2);

                if (asmType.getSort() == Type.ARRAY) {
                    Type elementType = correctElementType(asmType);
                    if (elementType.getSort() == Type.OBJECT || elementType.getSort() == Type.ARRAY) {
                        iv.invokestatic("java/util/Arrays", "equals", "([Ljava/lang/Object;[Ljava/lang/Object;)Z", false);
                    }
                    else {
                        iv.invokestatic("java/util/Arrays", "equals",
                                        "([" + elementType.getDescriptor() + "[" + elementType.getDescriptor() + ")Z", false);
                    }
                }
                else {
                    StackValue value = genEqualsForExpressionsOnStack(iv, JetTokens.EQEQ, asmType, asmType);
                    value.put(Type.BOOLEAN_TYPE, iv);
                }

                iv.ifeq(ne);
            }

            iv.mark(eq);
            iv.iconst(1);
            iv.areturn(Type.INT_TYPE);

            iv.mark(ne);
            iv.iconst(0);
            iv.areturn(Type.INT_TYPE);

            FunctionCodegen.endVisit(mv, "equals", myClass);
        }

        @Override
        public void generateHashCodeMethod(@NotNull List<PropertyDescriptor> properties) {
            FunctionDescriptor hashCodeFunction = CodegenUtil.getDeclaredFunctionByRawSignature(
                    descriptor, Name.identifier(CodegenUtil.HASH_CODE_METHOD_NAME),
                    KotlinBuiltIns.getInstance().getInt()
            );
            MethodContext context = ImplementationBodyCodegen.this.context.intoFunction(hashCodeFunction);
            MethodVisitor mv = v.newMethod(OtherOrigin(hashCodeFunction), ACC_PUBLIC, "hashCode", "()I", null, null);
            InstructionAdapter iv = new InstructionAdapter(mv);

            mv.visitCode();
            boolean first = true;
            for (PropertyDescriptor propertyDescriptor : properties) {
                if (!first) {
                    iv.iconst(31);
                    iv.mul(Type.INT_TYPE);
                }

                genPropertyOnStack(iv, context, propertyDescriptor, 0);

                Label ifNull = null;
                Type asmType = typeMapper.mapType(propertyDescriptor);
                if (!isPrimitive(asmType)) {
                    ifNull = new Label();
                    iv.dup();
                    iv.ifnull(ifNull);
                }

                genHashCode(mv, iv, asmType);

                if (ifNull != null) {
                    Label end = new Label();
                    iv.goTo(end);
                    iv.mark(ifNull);
                    iv.pop();
                    iv.iconst(0);
                    iv.mark(end);
                }

                if (first) {
                    first = false;
                }
                else {
                    iv.add(Type.INT_TYPE);
                }
            }

            mv.visitInsn(IRETURN);

            FunctionCodegen.endVisit(mv, "hashCode", myClass);
        }

        @Override
        public void generateToStringMethod(@NotNull List<PropertyDescriptor> properties) {
            FunctionDescriptor toString = CodegenUtil.getDeclaredFunctionByRawSignature(
                    descriptor, Name.identifier(CodegenUtil.TO_STRING_METHOD_NAME),
                    KotlinBuiltIns.getInstance().getString()
            );
            MethodContext context = ImplementationBodyCodegen.this.context.intoFunction(toString);
            MethodVisitor mv = v.newMethod(OtherOrigin(toString), ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
            InstructionAdapter iv = new InstructionAdapter(mv);

            mv.visitCode();
            genStringBuilderConstructor(iv);

            boolean first = true;
            for (PropertyDescriptor propertyDescriptor : properties) {
                if (first) {
                    iv.aconst(descriptor.getName() + "(" + propertyDescriptor.getName().asString()+"=");
                    first = false;
                }
                else {
                    iv.aconst(", " + propertyDescriptor.getName().asString() + "=");
                }
                genInvokeAppendMethod(iv, JAVA_STRING_TYPE);

                Type type = genPropertyOnStack(iv, context, propertyDescriptor, 0);

                if (type.getSort() == Type.ARRAY) {
                    Type elementType = correctElementType(type);
                    if (elementType.getSort() == Type.OBJECT || elementType.getSort() == Type.ARRAY) {
                        iv.invokestatic("java/util/Arrays", "toString", "([Ljava/lang/Object;)Ljava/lang/String;", false);
                        type = JAVA_STRING_TYPE;
                    }
                    else {
                        if (elementType.getSort() != Type.CHAR) {
                            iv.invokestatic("java/util/Arrays", "toString", "(" + type.getDescriptor() + ")Ljava/lang/String;", false);
                            type = JAVA_STRING_TYPE;
                        }
                    }
                }
                genInvokeAppendMethod(iv, type);
            }

            iv.aconst(")");
            genInvokeAppendMethod(iv, JAVA_STRING_TYPE);

            iv.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            iv.areturn(JAVA_STRING_TYPE);

            FunctionCodegen.endVisit(mv, "toString", myClass);
        }

        private Type genPropertyOnStack(InstructionAdapter iv, MethodContext context, PropertyDescriptor propertyDescriptor, int index) {
            iv.load(index, classAsmType);
            if (couldUseDirectAccessToProperty(propertyDescriptor, /* forGetter = */ true, /* isDelegated = */ false, context)) {
                Type type = typeMapper.mapType(propertyDescriptor.getType());
                String fieldName = ((FieldOwnerContext) context.getParentContext()).getFieldName(propertyDescriptor, false);
                iv.getfield(classAsmType.getInternalName(), fieldName, type.getDescriptor());
                return type.getReturnType();
            }
            else {
                //noinspection ConstantConditions
                Method method = typeMapper.mapSignature(propertyDescriptor.getGetter()).getAsmMethod();
                iv.invokevirtual(classAsmType.getInternalName(), method.getName(), method.getDescriptor(), false);
                return method.getReturnType();
            }
        }

        @Override
        public void generateComponentFunction(@NotNull FunctionDescriptor function, @NotNull final ValueParameterDescriptor parameter) {
            functionCodegen.generateMethod(OtherOrigin(myClass, function), typeMapper.mapSignature(function), function, new FunctionGenerationStrategy() {
                @Override
                public void generateBody(
                        @NotNull MethodVisitor mv,
                        @NotNull FrameMap frameMap,
                        @NotNull JvmMethodSignature signature,
                        @NotNull MethodContext context,
                        @NotNull MemberCodegen<?> parentCodegen
                ) {
                    Type componentType = signature.getReturnType();
                    InstructionAdapter iv = new InstructionAdapter(mv);
                    if (!componentType.equals(Type.VOID_TYPE)) {
                        PropertyDescriptor property =
                                bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, descriptorToDeclaration(parameter));
                        genPropertyOnStack(iv, context, property, 0);
                    }
                    iv.areturn(componentType);
                }
            });
        }

        @Override
        public void generateCopyFunction(@NotNull final FunctionDescriptor function, @NotNull List<JetParameter> constructorParameters) {
            JvmMethodSignature methodSignature = typeMapper.mapSignature(function);

            final Type thisDescriptorType = typeMapper.mapType(descriptor);

            functionCodegen.generateMethod(OtherOrigin(myClass, function), methodSignature, function, new FunctionGenerationStrategy() {
                @Override
                public void generateBody(
                        @NotNull MethodVisitor mv,
                        @NotNull FrameMap frameMap,
                        @NotNull JvmMethodSignature signature,
                        @NotNull MethodContext context,
                        @NotNull MemberCodegen<?> parentCodegen
                ) {
                    InstructionAdapter iv = new InstructionAdapter(mv);

                    iv.anew(thisDescriptorType);
                    iv.dup();

                    ConstructorDescriptor constructor = DeclarationResolver.getConstructorOfDataClass(descriptor);
                    assert function.getValueParameters().size() == constructor.getValueParameters().size() :
                            "Number of parameters of copy function and constructor are different. " +
                            "Copy: " + function.getValueParameters().size() + ", " +
                            "constructor: " + constructor.getValueParameters().size();

                    MutableClosure closure = ImplementationBodyCodegen.this.context.closure;
                    if (closure != null) {
                        ClassDescriptor captureThis = closure.getCaptureThis();
                        if (captureThis != null) {
                            iv.load(0, classAsmType);
                            Type type = typeMapper.mapType(captureThis);
                            iv.getfield(classAsmType.getInternalName(), CAPTURED_THIS_FIELD, type.getDescriptor());
                        }
                    }

                    int parameterIndex = 1; // localVariable 0 = this
                    for (ValueParameterDescriptor parameterDescriptor : function.getValueParameters()) {
                        Type type = typeMapper.mapType(parameterDescriptor.getType());
                        iv.load(parameterIndex, type);
                        parameterIndex += type.getSize();
                    }

                    Method constructorAsmMethod = typeMapper.mapSignature(constructor).getAsmMethod();
                    iv.invokespecial(thisDescriptorType.getInternalName(), "<init>", constructorAsmMethod.getDescriptor(), false);

                    iv.areturn(thisDescriptorType);
                }
            });

            functionCodegen.generateDefaultIfNeeded(
                    context.intoFunction(function), methodSignature, function, OwnerKind.IMPLEMENTATION,
                    new DefaultParameterValueLoader() {
                        @Override
                        public void putValueOnStack(ValueParameterDescriptor valueParameter, ExpressionCodegen codegen) {
                            assert KotlinBuiltIns.getInstance().isData((ClassDescriptor) function.getContainingDeclaration())
                                    : "Function container should be annotated with [data]: " + function;
                            PropertyDescriptor property = bindingContext.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, valueParameter);
                            assert property != null : "Copy function doesn't correspond to any property: " + function;
                            codegen.v.load(0, thisDescriptorType);
                            Type propertyType = typeMapper.mapType(property);
                            codegen.intermediateValueForProperty(property, false, null).put(propertyType, codegen.v);
                        }
                    },
                    null
            );
        }
    }

    private void generateEnumMethodsAndConstInitializers() {
        if (isEnumClass(descriptor)) {
            generateEnumValuesMethod();
            generateEnumValueOfMethod();
            initializeEnumConstants();
        }
    }

    private void generateEnumValuesMethod() {
        Type type = typeMapper.mapType(KotlinBuiltIns.getInstance().getArrayType(descriptor.getDefaultType()));

        FunctionDescriptor valuesFunction = findEnumFunction("values", new Function1<FunctionDescriptor, Boolean>() {
            @Override
            public Boolean invoke(FunctionDescriptor descriptor) {
                return isEnumValuesMethod(descriptor);
            }
        });
        MethodVisitor mv = v.newMethod(OtherOrigin(myClass, valuesFunction), ACC_PUBLIC | ACC_STATIC, "values", "()" + type.getDescriptor(), null, null);
        if (state.getClassBuilderMode() != ClassBuilderMode.FULL) return;

        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, classAsmType.getInternalName(), VALUES, type.getDescriptor());
        mv.visitMethodInsn(INVOKEVIRTUAL, type.getInternalName(), "clone", "()Ljava/lang/Object;", false);
        mv.visitTypeInsn(CHECKCAST, type.getInternalName());
        mv.visitInsn(ARETURN);
        FunctionCodegen.endVisit(mv, "values()", myClass);
    }

    private void generateEnumValueOfMethod() {
        FunctionDescriptor valueOfFunction = findEnumFunction("valueOf", new Function1<FunctionDescriptor, Boolean>() {
            @Override
            public Boolean invoke(FunctionDescriptor descriptor) {
                return isEnumValueOfMethod(descriptor);
            }
        });
        MethodVisitor mv = v.newMethod(OtherOrigin(myClass, valueOfFunction),
                                       ACC_PUBLIC | ACC_STATIC, "valueOf", "(Ljava/lang/String;)" + classAsmType.getDescriptor(), null, null);
        if (state.getClassBuilderMode() != ClassBuilderMode.FULL) return;

        mv.visitCode();
        mv.visitLdcInsn(classAsmType);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;", false);
        mv.visitTypeInsn(CHECKCAST, classAsmType.getInternalName());
        mv.visitInsn(ARETURN);
        FunctionCodegen.endVisit(mv, "valueOf()", myClass);
    }

    @NotNull
    private FunctionDescriptor findEnumFunction(@NotNull String name, Function1<FunctionDescriptor, Boolean> predicate) {
        ClassDescriptor enumClassObject = descriptor.getClassObjectDescriptor();
        assert enumClassObject != null : "No class object in " + descriptor;
        Collection<FunctionDescriptor> valuesFunctions = enumClassObject.getDefaultType().getMemberScope().getFunctions(Name.identifier(name));
        FunctionDescriptor valuesFunction = KotlinPackage.firstOrNull(valuesFunctions, predicate);
        assert valuesFunction != null : "No " + name + "() function found for " + descriptor;
        return valuesFunction;
    }

    protected void generateSyntheticAccessors() {
        Map<DeclarationDescriptor, DeclarationDescriptor> accessors = context.getAccessors();
        for (Map.Entry<DeclarationDescriptor, DeclarationDescriptor> entry : accessors.entrySet()) {
            generateSyntheticAccessor(entry);
        }
    }

    private void generateSyntheticAccessor(Map.Entry<DeclarationDescriptor, DeclarationDescriptor> entry) {
        if (entry.getValue() instanceof FunctionDescriptor) {
            FunctionDescriptor bridge = (FunctionDescriptor) entry.getValue();
            final FunctionDescriptor original = (FunctionDescriptor) entry.getKey();
            functionCodegen.generateMethod(
                    OtherOrigin(original), typeMapper.mapSignature(bridge), bridge,
                    new FunctionGenerationStrategy.CodegenBased<FunctionDescriptor>(state, bridge) {
                        @Override
                        public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                            generateMethodCallTo(original, codegen.v);
                            codegen.v.areturn(signature.getReturnType());
                        }
                    }
            );
        }
        else if (entry.getValue() instanceof PropertyDescriptor) {
            final PropertyDescriptor bridge = (PropertyDescriptor) entry.getValue();
            final PropertyDescriptor original = (PropertyDescriptor) entry.getKey();

            class PropertyAccessorStrategy extends FunctionGenerationStrategy.CodegenBased<PropertyAccessorDescriptor> {
                public PropertyAccessorStrategy(@NotNull PropertyAccessorDescriptor callableDescriptor) {
                    super(ImplementationBodyCodegen.this.state, callableDescriptor);
                }

                @Override
                public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                    boolean forceField = AsmUtil.isPropertyWithBackingFieldInOuterClass(original) &&
                                         !isClassObject(bridge.getContainingDeclaration());
                    StackValue property = codegen.intermediateValueForProperty(original, forceField, null, MethodKind.SYNTHETIC_ACCESSOR);

                    InstructionAdapter iv = codegen.v;
                    Type[] argTypes = signature.getAsmMethod().getArgumentTypes();
                    for (int i = 0, reg = 0; i < argTypes.length; i++) {
                        Type argType = argTypes[i];
                        iv.load(reg, argType);
                        //noinspection AssignmentToForLoopParameter
                        reg += argType.getSize();
                    }

                    if (callableDescriptor instanceof PropertyGetterDescriptor) {
                        property.put(property.type, iv);
                    }
                    else {
                        property.store(property.type, iv);
                    }

                    iv.areturn(signature.getReturnType());
                }
            }

            PropertyGetterDescriptor getter = bridge.getGetter();
            assert getter != null;
            functionCodegen.generateMethod(OtherOrigin(original.getGetter()), typeMapper.mapSignature(getter), getter,
                                           new PropertyAccessorStrategy(getter));


            if (bridge.isVar()) {
                PropertySetterDescriptor setter = bridge.getSetter();
                assert setter != null;

                functionCodegen.generateMethod(OtherOrigin(original.getSetter()), typeMapper.mapSignature(setter), setter,
                                               new PropertyAccessorStrategy(setter));
            }
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    private void generateMethodCallTo(FunctionDescriptor functionDescriptor, InstructionAdapter iv) {
        boolean isConstructor = functionDescriptor instanceof ConstructorDescriptor;
        boolean callFromAccessor = !JetTypeMapper.isAccessor(functionDescriptor);
        CallableMethod callableMethod = isConstructor ?
                                        typeMapper.mapToCallableMethod((ConstructorDescriptor) functionDescriptor) :
                                        typeMapper.mapToCallableMethod(functionDescriptor, callFromAccessor, context);

        int reg = 1;
        if (isConstructor) {
            iv.anew(callableMethod.getOwner());
            iv.dup();
            reg = 0;
        }
        else if (callFromAccessor) {
            iv.load(0, OBJECT_TYPE);
        }

        for (Type argType : callableMethod.getAsmMethod().getArgumentTypes()) {
            iv.load(reg, argType);
            reg += argType.getSize();
        }
        callableMethod.invokeWithoutAssertions(iv);
    }

    private void generateFieldForSingleton() {
        if (isEnumClass(descriptor) || isEnumEntry(descriptor)) return;

        ClassDescriptor classObjectDescriptor = descriptor.getClassObjectDescriptor();
        ClassDescriptor fieldTypeDescriptor;
        JetClassOrObject original;
        if (isObject(descriptor)) {
            original = myClass;
            fieldTypeDescriptor = descriptor;
        }
        else if (classObjectDescriptor != null) {
            JetClassObject classObject = ((JetClass) myClass).getClassObject();
            assert classObject != null : "Class object not found: " + myClass.getText();
            original = classObject.getObjectDeclaration();
            fieldTypeDescriptor = classObjectDescriptor;
        }
        else {
            return;
        }

        StackValue.Field field = StackValue.singleton(fieldTypeDescriptor, typeMapper);

        v.newField(OtherOrigin(original), ACC_PUBLIC | ACC_STATIC | ACC_FINAL, field.name, field.type.getDescriptor(), null, null);

        if (!AsmUtil.isClassObjectWithBackingFieldsInOuter(fieldTypeDescriptor)) {
            genInitSingleton(fieldTypeDescriptor, field);
        }
    }

    private void generateClassObjectBackingFieldCopies() {
        if (classObjectPropertiesToCopy == null) return;

        for (PropertyAndDefaultValue info : classObjectPropertiesToCopy) {
            PropertyDescriptor property = info.descriptor;

            Type type = typeMapper.mapType(property);
            FieldVisitor fv = v.newField(OtherOrigin(property), ACC_STATIC | ACC_FINAL | ACC_PUBLIC, context.getFieldName(property, false),
                                         type.getDescriptor(), typeMapper.mapFieldSignature(property.getType()),
                                         info.defaultValue);

            AnnotationCodegen.forField(fv, typeMapper).genAnnotations(property, type);

            //This field are always static and final so if it has constant initializer don't do anything in clinit,
            //field would be initialized via default value in v.newField(...) - see JVM SPEC Ch.4
            // TODO: test this code
            if (state.getClassBuilderMode() == ClassBuilderMode.FULL && info.defaultValue == null) {
                ExpressionCodegen codegen = createOrGetClInitCodegen();
                int classObjectIndex = putClassObjectInLocalVar(codegen);
                StackValue.local(classObjectIndex, OBJECT_TYPE).put(OBJECT_TYPE, codegen.v);
                copyFieldFromClassObject(property);
            }
        }
    }

    private int putClassObjectInLocalVar(ExpressionCodegen codegen) {
        FrameMap frameMap = codegen.myFrameMap;
        ClassDescriptor classObjectDescriptor = descriptor.getClassObjectDescriptor();
        int classObjectIndex = frameMap.getIndex(classObjectDescriptor);
        if (classObjectIndex == -1) {
            classObjectIndex = frameMap.enter(classObjectDescriptor, OBJECT_TYPE);
            StackValue classObject = StackValue.singleton(classObjectDescriptor, typeMapper);
            classObject.put(classObject.type, codegen.v);
            StackValue.local(classObjectIndex, classObject.type).store(classObject.type, codegen.v);
        }
        return classObjectIndex;
    }

    private void copyFieldFromClassObject(PropertyDescriptor propertyDescriptor) {
        ExpressionCodegen codegen = createOrGetClInitCodegen();
        StackValue property = codegen.intermediateValueForProperty(propertyDescriptor, false, null);
        property.put(property.type, codegen.v);
        StackValue.Field field = StackValue.field(property.type, classAsmType, propertyDescriptor.getName().asString(), true);
        field.store(field.type, codegen.v);
    }

    protected void genInitSingleton(ClassDescriptor fieldTypeDescriptor, StackValue.Field field) {
        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            Collection<ConstructorDescriptor> constructors = fieldTypeDescriptor.getConstructors();
            assert constructors.size() == 1 : "Class of singleton object must have only one constructor: " + constructors;

            ExpressionCodegen codegen = createOrGetClInitCodegen();
            FunctionDescriptor fd = codegen.accessibleFunctionDescriptor(constructors.iterator().next());
            generateMethodCallTo(fd, codegen.v);
            field.store(field.type, codegen.v);
        }
    }

    private void generatePrimaryConstructor(final DelegationFieldsInfo delegationFieldsInfo) {
        if (ignoreIfTraitOrAnnotation()) return;

        ConstructorDescriptor constructorDescriptor = bindingContext.get(BindingContext.CONSTRUCTOR, myClass);
        assert constructorDescriptor != null : "Constructor not found for class: " + descriptor;

        ConstructorContext constructorContext = context.intoConstructor(constructorDescriptor);

        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            lookupConstructorExpressionsInClosureIfPresent(constructorContext);
        }

        JvmMethodSignature signature = typeMapper.mapSignature(constructorDescriptor);

        functionCodegen.generateMethod(OtherOrigin(myClass, constructorDescriptor), signature, constructorDescriptor, constructorContext,
                   new FunctionGenerationStrategy.CodegenBased<ConstructorDescriptor>(state, constructorDescriptor) {
                       @Override
                       public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                           generatePrimaryConstructorImpl(callableDescriptor, codegen, delegationFieldsInfo);
                       }
                   }
        );

        functionCodegen.generateDefaultIfNeeded(constructorContext, signature, constructorDescriptor, OwnerKind.IMPLEMENTATION,
                                                DefaultParameterValueLoader.DEFAULT, null);

        CallableMethod callableMethod = typeMapper.mapToCallableMethod(constructorDescriptor);
        FunctionCodegen.generateConstructorWithoutParametersIfNeeded(state, callableMethod, constructorDescriptor, v);

        if (isClassObject(descriptor)) {
            context.recordSyntheticAccessorIfNeeded(constructorDescriptor, bindingContext);
        }
    }

    private void generatePrimaryConstructorImpl(
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull final ExpressionCodegen codegen,
            @NotNull DelegationFieldsInfo fieldsInfo
    ) {
        InstructionAdapter iv = codegen.v;

        MutableClosure closure = context.closure;
        if (closure != null) {
            List<FieldInfo> argsFromClosure = ClosureCodegen.calculateConstructorParameters(typeMapper, closure, classAsmType);
            int k = 1;
            for (FieldInfo info : argsFromClosure) {
                k = AsmUtil.genAssignInstanceFieldFromParam(info, k, iv);
            }
        }

        if (superCall == null) {
            genSimpleSuperCall(iv);
        }
        else {
            generateDelegatorToConstructorCall(iv, codegen, constructorDescriptor);
        }

        for (JetDelegationSpecifier specifier : myClass.getDelegationSpecifiers()) {
            if (specifier instanceof JetDelegatorByExpressionSpecifier) {
                genCallToDelegatorByExpressionSpecifier(iv, codegen, (JetDelegatorByExpressionSpecifier) specifier, fieldsInfo);
            }
        }

        int curParam = 0;
        List<ValueParameterDescriptor> parameters = constructorDescriptor.getValueParameters();
        for (JetParameter parameter : getPrimaryConstructorParameters()) {
            if (parameter.hasValOrVarNode()) {
                VariableDescriptor descriptor = parameters.get(curParam);
                Type type = typeMapper.mapType(descriptor);
                iv.load(0, classAsmType);
                iv.load(codegen.myFrameMap.getIndex(descriptor), type);
                PropertyDescriptor propertyDescriptor = bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
                assert propertyDescriptor != null : "Property descriptor is not found for primary constructor parameter: " + parameter;
                iv.putfield(classAsmType.getInternalName(), context.getFieldName(propertyDescriptor, false), type.getDescriptor());
            }
            curParam++;
        }

        boolean generateInitializerInOuter = isClassObjectWithBackingFieldsInOuter(descriptor);
        if (generateInitializerInOuter) {
            final ImplementationBodyCodegen parentCodegen = getParentBodyCodegen(this);
            //generate OBJECT$
            parentCodegen.genInitSingleton(descriptor, StackValue.singleton(descriptor, typeMapper));
            generateInitializers(new Function0<ExpressionCodegen>() {
                @Override
                public ExpressionCodegen invoke() {
                    return parentCodegen.createOrGetClInitCodegen();
                }
            });
        } else {
            generateInitializers(new Function0<ExpressionCodegen>() {
                @Override
                public ExpressionCodegen invoke() {
                    return codegen;
                }
            });
        }

        iv.visitInsn(RETURN);
    }

    private void genSimpleSuperCall(InstructionAdapter iv) {
        iv.load(0, superClassAsmType);
        if (descriptor.getKind() == ClassKind.ENUM_CLASS || descriptor.getKind() == ClassKind.ENUM_ENTRY) {
            iv.load(1, JAVA_STRING_TYPE);
            iv.load(2, Type.INT_TYPE);
            iv.invokespecial(superClassAsmType.getInternalName(), "<init>", "(Ljava/lang/String;I)V", false);
        }
        else {
            iv.invokespecial(superClassAsmType.getInternalName(), "<init>", "()V", false);
        }
    }

    private class DelegationFieldsInfo {
        private class Field {
            public final Type type;
            public final String name;
            public final boolean generateField;

            private Field(Type type, String name, boolean generateField) {
                this.type = type;
                this.name = name;
                this.generateField = generateField;
            }

            @NotNull
            public StackValue getStackValue() {
                return StackValue.field(type, classAsmType, name, false);
            }
        }
        private final Map<JetDelegatorByExpressionSpecifier, Field> fields = new HashMap<JetDelegatorByExpressionSpecifier, Field>();

        @NotNull
        public Field getInfo(JetDelegatorByExpressionSpecifier specifier) {
            return fields.get(specifier);
        }

        private void addField(JetDelegatorByExpressionSpecifier specifier, PropertyDescriptor propertyDescriptor) {
            fields.put(specifier,
                       new Field(typeMapper.mapType(propertyDescriptor), propertyDescriptor.getName().asString(), false));
        }

        private void addField(JetDelegatorByExpressionSpecifier specifier, Type type, String name) {
            fields.put(specifier, new Field(type, name, true));
        }
    }

    @NotNull
    private DelegationFieldsInfo getDelegationFieldsInfo(@NotNull List<JetDelegationSpecifier> delegationSpecifiers) {
        DelegationFieldsInfo result = new DelegationFieldsInfo();
        int n = 0;
        for (JetDelegationSpecifier specifier : delegationSpecifiers) {
            if (specifier instanceof JetDelegatorByExpressionSpecifier) {
                JetExpression expression = ((JetDelegatorByExpressionSpecifier) specifier).getDelegateExpression();
                PropertyDescriptor propertyDescriptor = getDelegatePropertyIfAny(expression);

                ClassDescriptor superClassDescriptor = getSuperClass(specifier);

                if (propertyDescriptor != null &&
                    !propertyDescriptor.isVar() &&
                    Boolean.TRUE.equals(bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor))) {
                    // final property with backing field
                    result.addField((JetDelegatorByExpressionSpecifier) specifier, propertyDescriptor);
                }
                else {
                    result.addField((JetDelegatorByExpressionSpecifier) specifier, typeMapper.mapType(superClassDescriptor), "$delegate_" + n);
                }
                n++;
            }
        }
        return result;
    }

    @NotNull
    private ClassDescriptor getSuperClass(@NotNull JetDelegationSpecifier specifier) {
        JetType superType = bindingContext.get(BindingContext.TYPE, specifier.getTypeReference());
        assert superType != null;

        ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
        assert superClassDescriptor != null;
        return superClassDescriptor;
    }

    private void genCallToDelegatorByExpressionSpecifier(
            InstructionAdapter iv,
            ExpressionCodegen codegen,
            JetDelegatorByExpressionSpecifier specifier,
            DelegationFieldsInfo fieldsInfo
    ) {
        JetExpression expression = specifier.getDelegateExpression();

        DelegationFieldsInfo.Field fieldInfo = fieldsInfo.getInfo(specifier);
        if (fieldInfo.generateField) {
            iv.load(0, classAsmType);
            codegen.genToJVMStack(expression);

            fieldInfo.getStackValue().store(fieldInfo.type, iv);
        }
    }

    @Nullable
    private PropertyDescriptor getDelegatePropertyIfAny(JetExpression expression) {
        PropertyDescriptor propertyDescriptor = null;
        if (expression instanceof JetSimpleNameExpression) {
            ResolvedCall<?> call = BindingContextUtilPackage.getResolvedCall(expression, bindingContext);
            if (call != null) {
                CallableDescriptor callResultingDescriptor = call.getResultingDescriptor();
                if (callResultingDescriptor instanceof ValueParameterDescriptor) {
                    ValueParameterDescriptor valueParameterDescriptor = (ValueParameterDescriptor) callResultingDescriptor;
                    // constructor parameter
                    if (valueParameterDescriptor.getContainingDeclaration() instanceof ConstructorDescriptor) {
                        // constructor of my class
                        if (valueParameterDescriptor.getContainingDeclaration().getContainingDeclaration() == descriptor) {
                            propertyDescriptor = bindingContext.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, valueParameterDescriptor);
                        }
                    }
                }

                // todo: when and if frontend will allow properties defined not as constructor parameters to be used in delegation specifier
            }
        }
        return propertyDescriptor;
    }

    private void lookupConstructorExpressionsInClosureIfPresent(final ConstructorContext constructorContext) {
        JetVisitorVoid visitor = new JetVisitorVoid() {
            @Override
            public void visitJetElement(@NotNull JetElement e) {
                e.acceptChildren(this);
            }

            @Override
            public void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expr) {
                DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expr);

                DeclarationDescriptor toLookup;
                if (isLocalNamedFun(descriptor)) {
                    toLookup = descriptor;
                }
                else if (descriptor instanceof CallableMemberDescriptor) {
                    toLookup = descriptor.getContainingDeclaration();
                }
                else if (descriptor instanceof VariableDescriptor) {
                    ConstructorDescriptor constructorDescriptor = (ConstructorDescriptor) constructorContext.getContextDescriptor();
                    for (ValueParameterDescriptor parameterDescriptor : constructorDescriptor.getValueParameters()) {
                        if (descriptor.equals(parameterDescriptor)) return;
                    }
                    toLookup = descriptor;
                }
                else return;

                constructorContext.lookupInContext(toLookup, null, state, true);
            }

            @Override
            public void visitThisExpression(@NotNull JetThisExpression expression) {
                DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getInstanceReference());
                assert descriptor instanceof CallableDescriptor ||
                       descriptor instanceof ClassDescriptor : "'This' reference target should be class or callable descriptor but was " + descriptor;
                if (context.getCallableDescriptorWithReceiver() != descriptor) {
                    context.lookupInContext(descriptor, null, state, true);
                }
            }
        };

        for (JetDeclaration declaration : myClass.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                JetProperty property = (JetProperty) declaration;
                JetExpression initializer = property.getInitializer();
                if (initializer != null) {
                    initializer.accept(visitor);
                }
            }
            else if (declaration instanceof JetClassInitializer) {
                JetClassInitializer initializer = (JetClassInitializer) declaration;
                initializer.accept(visitor);
            }
        }

        for (JetDelegationSpecifier specifier : myClass.getDelegationSpecifiers()) {
            if (specifier instanceof JetDelegatorByExpressionSpecifier) {
                JetExpression delegateExpression = ((JetDelegatorByExpressionSpecifier) specifier).getDelegateExpression();
                assert delegateExpression != null;
                delegateExpression.accept(visitor);
            }
        }

        if (superCall != null && !isAnonymousObject(descriptor)) {
            JetValueArgumentList argumentList = superCall.getValueArgumentList();
            if (argumentList != null) {
                argumentList.accept(visitor);
            }
        }
    }

    private boolean ignoreIfTraitOrAnnotation() {
        if (myClass instanceof JetClass) {
            JetClass aClass = (JetClass) myClass;
            if (aClass.isTrait()) {
                return true;
            }
            if (aClass.isAnnotation()) {
                return true;
            }
        }
        return false;
    }

    private void generateTraitMethods() {
        if (JetPsiUtil.isTrait(myClass)) return;

        for (DeclarationDescriptor declaration : descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            if (!(declaration instanceof CallableMemberDescriptor)) continue;

            CallableMemberDescriptor inheritedMember = (CallableMemberDescriptor) declaration;
            CallableMemberDescriptor traitMember = BridgesPackage.findTraitImplementation(inheritedMember);
            if (traitMember == null) continue;

            assert traitMember.getModality() != Modality.ABSTRACT : "Cannot delegate to abstract trait method: " + inheritedMember;

            // inheritedMember can be abstract here. In order for FunctionCodegen to generate the method body, we're creating a copy here
            // with traitMember's modality
            CallableMemberDescriptor copy =
                    inheritedMember.copy(inheritedMember.getContainingDeclaration(), traitMember.getModality(), Visibilities.PUBLIC,
                                         CallableMemberDescriptor.Kind.DECLARATION, true);

            if (traitMember instanceof SimpleFunctionDescriptor) {
                generateDelegationToTraitImpl((FunctionDescriptor) traitMember, (FunctionDescriptor) copy);
            }
            else if (traitMember instanceof PropertyDescriptor) {
                for (PropertyAccessorDescriptor traitAccessor : ((PropertyDescriptor) traitMember).getAccessors()) {
                    for (PropertyAccessorDescriptor inheritedAccessor : ((PropertyDescriptor) copy).getAccessors()) {
                        if (inheritedAccessor.getClass() == traitAccessor.getClass()) { // same accessor kind
                            generateDelegationToTraitImpl(traitAccessor, inheritedAccessor);
                        }
                    }
                }
            }
        }
    }

    private void generateDelegationToTraitImpl(@NotNull final FunctionDescriptor traitFun, @NotNull FunctionDescriptor inheritedFun) {
        functionCodegen.generateMethod(
                DelegationToTraitImpl(descriptorToDeclaration(traitFun), traitFun),
                typeMapper.mapSignature(inheritedFun),
                inheritedFun,
                new FunctionGenerationStrategy.CodegenBased<FunctionDescriptor>(state, inheritedFun) {
                    @Override
                    public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                        DeclarationDescriptor containingDeclaration = traitFun.getContainingDeclaration();
                        if (!DescriptorUtils.isTrait(containingDeclaration)) return;
                        ClassDescriptor containingTrait = (ClassDescriptor) containingDeclaration;
                        Type traitImplType = typeMapper.mapTraitImpl(containingTrait);

                        Method traitMethod = typeMapper.mapSignature(traitFun.getOriginal(), OwnerKind.TRAIT_IMPL).getAsmMethod();

                        Type[] argTypes = signature.getAsmMethod().getArgumentTypes();
                        Type[] originalArgTypes = traitMethod.getArgumentTypes();
                        assert originalArgTypes.length == argTypes.length + 1 :
                                "Invalid trait implementation signature: " + signature + " vs " + traitMethod + " for " + traitFun;

                        InstructionAdapter iv = codegen.v;
                        iv.load(0, OBJECT_TYPE);
                        for (int i = 0, reg = 1; i < argTypes.length; i++) {
                            StackValue.local(reg, argTypes[i]).put(originalArgTypes[i + 1], iv);
                            //noinspection AssignmentToForLoopParameter
                            reg += argTypes[i].getSize();
                        }

                        if (KotlinBuiltIns.getInstance().isCloneable(containingTrait) && traitMethod.getName().equals("clone")) {
                            // A special hack for Cloneable: there's no kotlin/Cloneable$$TImpl class at runtime,
                            // and its 'clone' method is actually located in java/lang/Object
                            iv.invokespecial("java/lang/Object", "clone", "()Ljava/lang/Object;", false);
                        }
                        else {
                            iv.invokestatic(traitImplType.getInternalName(), traitMethod.getName(), traitMethod.getDescriptor(), false);
                        }

                        Type returnType = signature.getReturnType();
                        StackValue.onStack(traitMethod.getReturnType()).put(returnType, iv);
                        iv.areturn(returnType);
                    }
                }
        );
    }

    private void generateDelegatorToConstructorCall(
            @NotNull InstructionAdapter iv,
            @NotNull ExpressionCodegen codegen,
            @NotNull ConstructorDescriptor constructorDescriptor
    ) {
        iv.load(0, OBJECT_TYPE);

        ResolvedCall<?> resolvedCall = getResolvedCallWithAssert(superCall, bindingContext);
        ConstructorDescriptor superConstructor = (ConstructorDescriptor) resolvedCall.getResultingDescriptor();

        CallableMethod superCallable = typeMapper.mapToCallableMethod(superConstructor);
        CallableMethod callable = typeMapper.mapToCallableMethod(constructorDescriptor);

        List<JvmMethodParameterSignature> superParameters = superCallable.getValueParameters();
        List<JvmMethodParameterSignature> parameters = callable.getValueParameters();

        int offset = 1;
        int superIndex = 0;

        // Here we match all the super constructor parameters except those with kind VALUE to the derived constructor parameters, push
        // them all onto the stack and update "offset" variable so that in the end it points to the slot of the first VALUE argument
        for (JvmMethodParameterSignature parameter : parameters) {
            if (superIndex >= superParameters.size()) break;

            JvmMethodParameterKind kind = parameter.getKind();
            Type type = parameter.getAsmType();

            // Stop when we reach the actual value parameters present in the code; they will be generated via ResolvedCall below
            if (superParameters.get(superIndex).getKind() == JvmMethodParameterKind.VALUE &&
                kind == JvmMethodParameterKind.SUPER_CALL_PARAM) {
                break;
            }

            if (kind == JvmMethodParameterKind.SUPER_CALL_PARAM || kind == JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL ||
                (kind == JvmMethodParameterKind.OUTER && superConstructor.getContainingDeclaration().isInner())) {
                iv.load(offset, type);
                superIndex++;
            }

            offset += type.getSize();
        }

        ArgumentGenerator argumentGenerator;
        if (isAnonymousObject(descriptor)) {
            List<JvmMethodParameterSignature> superValues = superParameters.subList(superIndex, superParameters.size());
            argumentGenerator = new ObjectSuperCallArgumentGenerator(superValues, iv, offset);
        }
        else {
            argumentGenerator =
                    new CallBasedArgumentGenerator(codegen, codegen.defaultCallGenerator, superConstructor.getValueParameters(),
                                                   superCallable.getValueParameterTypes());
        }

        codegen.invokeMethodWithArguments(superCallable, resolvedCall, StackValue.none(), codegen.defaultCallGenerator, argumentGenerator);
    }

    private static class ObjectSuperCallArgumentGenerator extends ArgumentGenerator {
        private final List<JvmMethodParameterSignature> parameters;
        private final InstructionAdapter iv;
        private int offset;

        public ObjectSuperCallArgumentGenerator(
                @NotNull List<JvmMethodParameterSignature> superParameters,
                @NotNull InstructionAdapter iv,
                int firstValueParamOffset
        ) {
            this.parameters = superParameters;
            this.iv = iv;
            this.offset = firstValueParamOffset;
        }

        @Override
        public void generateExpression(int i, @NotNull ExpressionValueArgument argument) {
            generateSuperCallArgument(i);
        }

        @Override
        public void generateDefault(int i, @NotNull DefaultValueArgument argument) {
            pushDefaultValueOnStack(parameters.get(i).getAsmType(), iv);
        }

        @Override
        public void generateVararg(int i, @NotNull VarargValueArgument argument) {
            generateSuperCallArgument(i);
        }

        private void generateSuperCallArgument(int i) {
            Type type = parameters.get(i).getAsmType();
            iv.load(offset, type);
            offset += type.getSize();
        }
    }

    @Override
    protected void generateDeclaration(PropertyCodegen propertyCodegen, JetDeclaration declaration) {
        if (declaration instanceof JetEnumEntry) {
            String name = declaration.getName();
            assert name != null : "Enum entry has no name: " + declaration.getText();
            String desc = classAsmType.getDescriptor();
            ClassDescriptor entryDescriptor = bindingContext.get(BindingContext.CLASS, declaration);
            v.newField(OtherOrigin(declaration, entryDescriptor), ACC_PUBLIC | ACC_ENUM | ACC_STATIC | ACC_FINAL, name, desc, null, null);
            myEnumConstants.add((JetEnumEntry) declaration);
        }

        super.generateDeclaration(propertyCodegen, declaration);
    }

    private final List<JetEnumEntry> myEnumConstants = new ArrayList<JetEnumEntry>();

    private void initializeEnumConstants() {
        if (state.getClassBuilderMode() != ClassBuilderMode.FULL) return;

        ExpressionCodegen codegen = createOrGetClInitCodegen();
        InstructionAdapter iv = codegen.v;

        Type arrayAsmType = typeMapper.mapType(KotlinBuiltIns.getInstance().getArrayType(descriptor.getDefaultType()));
        v.newField(OtherOrigin(myClass), ACC_PRIVATE | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC, VALUES, arrayAsmType.getDescriptor(), null, null);

        iv.iconst(myEnumConstants.size());
        iv.newarray(classAsmType);

        if (!myEnumConstants.isEmpty()) {
            iv.dup();
            for (int ordinal = 0, size = myEnumConstants.size(); ordinal < size; ordinal++) {
                initializeEnumConstant(codegen, ordinal);
            }
        }

        iv.putstatic(classAsmType.getInternalName(), VALUES, arrayAsmType.getDescriptor());
    }

    private void initializeEnumConstant(@NotNull ExpressionCodegen codegen, int ordinal) {
        InstructionAdapter iv = codegen.v;
        JetEnumEntry enumConstant = myEnumConstants.get(ordinal);

        iv.dup();
        iv.iconst(ordinal);

        ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, enumConstant);
        assert classDescriptor != null;
        Type implClass = typeMapper.mapClass(classDescriptor);

        List<JetDelegationSpecifier> delegationSpecifiers = enumConstant.getDelegationSpecifiers();
        if (delegationSpecifiers.size() > 1) {
            throw new UnsupportedOperationException("multiple delegation specifiers for enum constant not supported");
        }

        iv.anew(implClass);
        iv.dup();

        iv.aconst(enumConstant.getName());
        iv.iconst(ordinal);

        if (delegationSpecifiers.size() == 1 && !enumEntryNeedSubclass(state.getBindingContext(), enumConstant)) {
            JetDelegationSpecifier specifier = delegationSpecifiers.get(0);
            if (!(specifier instanceof JetDelegatorToSuperCall)) {
                throw new UnsupportedOperationException("unsupported type of enum constant initializer: " + specifier);
            }

            ResolvedCall<?> resolvedCall = getResolvedCallWithAssert(specifier, bindingContext);

            CallableMethod method = typeMapper.mapToCallableMethod((ConstructorDescriptor) resolvedCall.getResultingDescriptor());

            codegen.invokeMethodWithArguments(method, resolvedCall, StackValue.none());
        }
        else {
            iv.invokespecial(implClass.getInternalName(), "<init>", "(Ljava/lang/String;I)V", false);
        }

        iv.dup();
        iv.putstatic(classAsmType.getInternalName(), enumConstant.getName(), classAsmType.getDescriptor());
        iv.astore(OBJECT_TYPE);
    }

    private void generateDelegates(DelegationFieldsInfo delegationFieldsInfo) {
        for (JetDelegationSpecifier specifier : myClass.getDelegationSpecifiers()) {
            if (specifier instanceof JetDelegatorByExpressionSpecifier) {
                DelegationFieldsInfo.Field field = delegationFieldsInfo.getInfo((JetDelegatorByExpressionSpecifier) specifier);
                generateDelegateField(field);
                generateDelegates(getSuperClass(specifier), field);
            }
        }
    }

    private void generateDelegateField(DelegationFieldsInfo.Field fieldInfo) {
        if (!fieldInfo.generateField) return;

        v.newField(JvmDeclarationOrigin.NO_ORIGIN, ACC_PRIVATE | ACC_FINAL | ACC_SYNTHETIC,
                   fieldInfo.name, fieldInfo.type.getDescriptor(), /*TODO*/null, null);
    }

    protected void generateDelegates(ClassDescriptor toClass, DelegationFieldsInfo.Field field) {
        for (DeclarationDescriptor declaration : descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            if (declaration instanceof CallableMemberDescriptor) {
                CallableMemberDescriptor callableMemberDescriptor = (CallableMemberDescriptor) declaration;
                if (callableMemberDescriptor.getKind() == CallableMemberDescriptor.Kind.DELEGATION) {
                    Set<? extends CallableMemberDescriptor> overriddenDescriptors = callableMemberDescriptor.getOverriddenDescriptors();
                    for (CallableMemberDescriptor overriddenDescriptor : overriddenDescriptors) {
                        if (overriddenDescriptor.getContainingDeclaration() == toClass) {
                            if (declaration instanceof PropertyDescriptor) {
                                propertyCodegen
                                        .genDelegate((PropertyDescriptor) declaration, (PropertyDescriptor) overriddenDescriptor, field.getStackValue());
                            }
                            else if (declaration instanceof FunctionDescriptor) {
                                functionCodegen
                                        .genDelegate((FunctionDescriptor) declaration, (FunctionDescriptor) overriddenDescriptor, field.getStackValue());
                            }
                        }
                    }
                }
            }
        }
    }

    public void addClassObjectPropertyToCopy(PropertyDescriptor descriptor, Object defaultValue) {
        if (classObjectPropertiesToCopy == null) {
            classObjectPropertiesToCopy = new ArrayList<PropertyAndDefaultValue>();
        }
        classObjectPropertiesToCopy.add(new PropertyAndDefaultValue(descriptor, defaultValue));
    }

    private static class PropertyAndDefaultValue {
        public final PropertyDescriptor descriptor;
        public final Object defaultValue;

        public PropertyAndDefaultValue(PropertyDescriptor descriptor, Object defaultValue) {
            this.descriptor = descriptor;
            this.defaultValue = defaultValue;
        }
    }
}
