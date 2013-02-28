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

package org.jetbrains.jet.codegen;

import com.google.common.collect.Lists;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.*;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.jet.codegen.binding.CalculatedClosure;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.binding.MutableClosure;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.ConstructorContext;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.signature.*;
import org.jetbrains.jet.codegen.signature.kotlin.JetMethodAnnotationWriter;
import org.jetbrains.jet.codegen.signature.kotlin.JetValueParameterAnnotationWriter;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.codegen.state.JetTypeMapperMode;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.OverridingUtil;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.kt.DescriptorKindUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.*;
import static org.jetbrains.jet.codegen.CodegenUtil.*;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.*;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.callableDescriptorToDeclaration;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.JAVA_STRING_TYPE;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

public class ImplementationBodyCodegen extends ClassBodyCodegen {
    private static final String VALUES = "$VALUES";
    private JetDelegationSpecifier superCall;
    private Type superClassAsmType;
    @Nullable // null means java/lang/Object
    private JetType superClassType;
    private final Type classAsmType;

    public ImplementationBodyCodegen(JetClassOrObject aClass, CodegenContext context, ClassBuilder v, GenerationState state) {
        super(aClass, context, v, state);
        classAsmType = typeMapper.mapType(descriptor.getDefaultType(), JetTypeMapperMode.IMPL);
    }

    @Override
    protected void generateDeclaration() {
        getSuperClass();

        JvmClassSignature signature = signature();

        boolean isAbstract = false;
        boolean isInterface = false;
        boolean isFinal = false;
        boolean isStatic = false;
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

        if (state.getClassBuilderMode() == ClassBuilderMode.SIGNATURES && !DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            // ClassBuilderMode.SIGNATURES means we are generating light classes & looking at a nested or inner class
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
                      interfaces.toArray(new String[interfaces.size()])
        );
        v.visitSource(myClass.getContainingFile().getName(), null);

        writeOuterClass();

        writeInnerClasses();

        AnnotationCodegen.forClass(v.getVisitor(), typeMapper).genAnnotations(descriptor);

        writeClassSignatureIfNeeded(signature);
    }

    private void writeOuterClass() {
        //JVMS7: A class must have an EnclosingMethod attribute if and only if it is a local class or an anonymous class.
        DeclarationDescriptor parentDescriptor = descriptor.getContainingDeclaration();

        boolean isObjectLiteral = descriptor.getName().isSpecial() && descriptor.getKind() == ClassKind.OBJECT;

        boolean isLocalOrAnonymousClass = isObjectLiteral ||
                                          !(parentDescriptor instanceof NamespaceDescriptor || parentDescriptor instanceof ClassDescriptor);
        if (isLocalOrAnonymousClass) {
            String outerClassName = getOuterClassName(descriptor, typeMapper, bindingContext, state);
            FunctionDescriptor function = DescriptorUtils.getParentOfType(descriptor, FunctionDescriptor.class);

            //Function descriptor could be null only for object literal in package namespace
            assert (!isObjectLiteral && function != null) || isObjectLiteral:
                    "Function descriptor should be present: " + descriptor.getName();

            Name functionName = function != null ? function.getName() : null;

            v.visitOuterClass(outerClassName,
                              functionName != null ? functionName.getName() : null,
                              functionName != null ? typeMapper.mapSignature(functionName, function).getAsmMethod().getDescriptor() : null);

        }
    }

    @NotNull
    public static String getOuterClassName(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull JetTypeMapper typeMapper,
            @NotNull BindingContext bindingContext,
            @NotNull GenerationState state
    ) {
        ClassDescriptor container = DescriptorUtils.getParentOfType(classDescriptor, ClassDescriptor.class);
        if (container != null) {
            return typeMapper.mapType(container.getDefaultType(), JetTypeMapperMode.IMPL).getInternalName();
        }
        else {
            NamespaceDescriptor namespaceDescriptor = DescriptorUtils.getParentOfType(classDescriptor, NamespaceDescriptor.class);
            assert namespaceDescriptor != null : "Namespace descriptor should be present: " + classDescriptor.getName();
            FqName namespaceQN = namespaceDescriptor.getFqName();
            boolean isMultiFile = CodegenBinding.isMultiFileNamespace(state.getBindingContext(), namespaceQN);
            return isMultiFile
                   ? NamespaceCodegen.getNamespacePartInternalName(
                                     BindingContextUtils.getContainingFile(bindingContext, classDescriptor))
                   : NamespaceCodegen.getJVMClassNameForKotlinNs(namespaceQN).getInternalName();
        }
    }

    private void writeInnerClasses() {
        for (ClassDescriptor innerClass : getInnerClassesAndObjects(descriptor)) {
            writeInnerClass(innerClass);
        }

        ClassDescriptor classObjectDescriptor = descriptor.getClassObjectDescriptor();
        if (classObjectDescriptor != null) {
            int innerClassAccess = getVisibilityAccessFlag(classObjectDescriptor) | ACC_FINAL | ACC_STATIC;
            v.visitInnerClass(classAsmType.getInternalName() + JvmAbi.CLASS_OBJECT_SUFFIX, classAsmType.getInternalName(),
                              JvmAbi.CLASS_OBJECT_CLASS_NAME,
                              innerClassAccess);
        }
    }

    private void writeInnerClass(ClassDescriptor innerClass) {
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
        String outerClassInternalName = classAsmType.getInternalName();
        String innerClassInternalName = typeMapper.mapType(innerClass.getDefaultType(), JetTypeMapperMode.IMPL).getInternalName();
        v.visitInnerClass(innerClassInternalName, outerClassInternalName, innerClass.getName().getName(), innerClassAccess);
    }

    private void writeClassSignatureIfNeeded(JvmClassSignature signature) {
        AnnotationVisitor annotationVisitor = v.newAnnotation(JvmStdlibNames.JET_CLASS.getDescriptor(), true);
        annotationVisitor.visit(JvmStdlibNames.JET_CLASS_SIGNATURE, signature.getKotlinGenericSignature());
        int flags = getFlagsForVisibility(descriptor.getVisibility()) | getFlagsForClassKind(descriptor);
        if (JvmStdlibNames.FLAGS_DEFAULT_VALUE != flags) {
            annotationVisitor.visit(JvmStdlibNames.JET_FLAGS_FIELD, flags);
        }
        annotationVisitor.visit(JvmStdlibNames.ABI_VERSION_NAME, JvmAbi.VERSION);
        annotationVisitor.visitEnd();
    }

    private JvmClassSignature signature() {
        List<String> superInterfaces;

        LinkedHashSet<String> superInterfacesLinkedHashSet = new LinkedHashSet<String>();

        // TODO: generics signature is not always needed
        BothSignatureWriter signatureVisitor = new BothSignatureWriter(BothSignatureWriter.Mode.CLASS, true);


        {   // type parameters
            List<TypeParameterDescriptor> typeParameters = descriptor.getTypeConstructor().getParameters();
            typeMapper.writeFormalTypeParameters(typeParameters, signatureVisitor);
        }

        signatureVisitor.writeSupersStart();

        {   // superclass
            signatureVisitor.writeSuperclass();
            if (superClassType == null) {
                signatureVisitor.writeClassBegin(superClassAsmType.getInternalName(), false, false);
                signatureVisitor.writeClassEnd();
            }
            else {
                typeMapper.mapType(superClassType, signatureVisitor, JetTypeMapperMode.TYPE_PARAMETER);
            }
            signatureVisitor.writeSuperclassEnd();
        }


        {   // superinterfaces
            superInterfacesLinkedHashSet.add(JvmStdlibNames.JET_OBJECT.getInternalName());

            for (JetDelegationSpecifier specifier : myClass.getDelegationSpecifiers()) {
                JetType superType = bindingContext.get(BindingContext.TYPE, specifier.getTypeReference());
                assert superType != null;
                ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
                if (isInterface(superClassDescriptor)) {
                    signatureVisitor.writeInterface();
                    Type jvmName = typeMapper.mapType(superType, signatureVisitor, JetTypeMapperMode.TYPE_PARAMETER);
                    signatureVisitor.writeInterfaceEnd();
                    superInterfacesLinkedHashSet.add(jvmName.getInternalName());
                }
            }

            superInterfaces = new ArrayList<String>(superInterfacesLinkedHashSet);
        }

        signatureVisitor.writeSupersEnd();

        return new JvmClassSignature(jvmName(), superClassAsmType.getInternalName(), superInterfaces, signatureVisitor.makeJavaGenericSignature(),
                                     signatureVisitor.makeKotlinClassSignature());
    }

    private String jvmName() {
        if (kind != OwnerKind.IMPLEMENTATION) {
            throw new IllegalStateException("must not call this method with kind " + kind);
        }
        return classAsmType.getInternalName();
    }

    protected void getSuperClass() {
        superClassAsmType = AsmTypeConstants.OBJECT_TYPE;
        superClassType = null;

        List<JetDelegationSpecifier> delegationSpecifiers = myClass.getDelegationSpecifiers();

        if (myClass instanceof JetClass && ((JetClass) myClass).isTrait()) {
            return;
        }

        if (kind != OwnerKind.IMPLEMENTATION) {
            throw new IllegalStateException("must be impl to reach this code: " + kind);
        }

        for (JetDelegationSpecifier specifier : delegationSpecifiers) {
            if (specifier instanceof JetDelegatorToSuperClass || specifier instanceof JetDelegatorToSuperCall) {
                JetType superType = bindingContext.get(BindingContext.TYPE, specifier.getTypeReference());
                assert superType != null;
                ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
                assert superClassDescriptor != null;
                if (!isInterface(superClassDescriptor)) {
                    superClassType = superType;
                    superClassAsmType = typeMapper.mapType(superClassDescriptor.getDefaultType(), JetTypeMapperMode.IMPL);
                    superCall = specifier;
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
        generateFieldForSingleton();

        try {
            generatePrimaryConstructor();
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

        generateAccessors();

        generateEnumMethods();

        generateFunctionsForDataClasses();

        genClosureFields(context.closure, v, state.getTypeMapper());
    }

    private List<PropertyDescriptor> getDataProperties() {
        ArrayList<PropertyDescriptor> result = Lists.newArrayList();
        for (JetParameter parameter : getPrimaryConstructorParameters()) {
            if (parameter.getValOrVarNode() == null) continue;

            PropertyDescriptor propertyDescriptor = state.getBindingContext().get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
            assert propertyDescriptor != null;

            result.add(propertyDescriptor);
        }
        return result;
    }

    private void generateFunctionsForDataClasses() {
        if (!KotlinBuiltIns.getInstance().isData(descriptor)) return;

        generateComponentFunctionsForDataClasses();
        generateCopyFunctionForDataClasses();

        List<PropertyDescriptor> properties = getDataProperties();
        if (!properties.isEmpty()) {
            generateDataClassToStringIfNeeded(properties);
            generateDataClassHashCodeIfNeeded(properties);
            generateDataClassEqualsIfNeeded(properties);
        }
    }

    private void generateCopyFunctionForDataClasses() {
        FunctionDescriptor copyFunction = bindingContext.get(BindingContext.DATA_CLASS_COPY_FUNCTION, descriptor);
        if (copyFunction != null) {
            generateCopyFunction(copyFunction);
        }
    }

    private void generateDataClassToStringIfNeeded(List<PropertyDescriptor> properties) {
        ClassDescriptor stringClass = KotlinBuiltIns.getInstance().getString();
        if (getDeclaredFunctionByRawSignature(descriptor, Name.identifier("toString"), stringClass) == null) {
            generateDataClassToStringMethod(properties);
        }
    }

    private void generateDataClassHashCodeIfNeeded(List<PropertyDescriptor> properties) {
        ClassDescriptor intClass = KotlinBuiltIns.getInstance().getInt();
        if (getDeclaredFunctionByRawSignature(descriptor, Name.identifier("hashCode"), intClass) == null) {
            generateDataClassHashCodeMethod(properties);
        }
    }

    private void generateDataClassEqualsIfNeeded(List<PropertyDescriptor> properties) {
        ClassDescriptor booleanClass = KotlinBuiltIns.getInstance().getBoolean();
        ClassDescriptor anyClass = KotlinBuiltIns.getInstance().getAny();
        FunctionDescriptor equalsFunction = getDeclaredFunctionByRawSignature(descriptor, Name.identifier("equals"), booleanClass, anyClass);
        if (equalsFunction == null) {
            generateDataClassEqualsMethod(properties);
        }
    }

    private void generateDataClassEqualsMethod(List<PropertyDescriptor> properties) {
        final MethodVisitor mv = v.getVisitor().visitMethod(ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
        final InstructionAdapter iv = new InstructionAdapter(mv);

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
            Type asmType = typeMapper.mapType(propertyDescriptor.getType());

            genPropertyOnStack(iv, propertyDescriptor, 0);
            genPropertyOnStack(iv, propertyDescriptor, 2);

            if (asmType.getSort() == Type.ARRAY) {
                final Type elementType = correctElementType(asmType);
                if (elementType.getSort() == Type.OBJECT || elementType.getSort() == Type.ARRAY) {
                    iv.invokestatic("java/util/Arrays", "equals", "([Ljava/lang/Object;[Ljava/lang/Object;)Z");
                }
                else {
                    iv.invokestatic("java/util/Arrays", "equals", "([" + elementType.getDescriptor() + "[" + elementType.getDescriptor() + ")Z");
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

    private void generateDataClassHashCodeMethod(List<PropertyDescriptor> properties) {
        final MethodVisitor mv = v.getVisitor().visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
        final InstructionAdapter iv = new InstructionAdapter(mv);

        mv.visitCode();
        boolean first = true;
        for (PropertyDescriptor propertyDescriptor : properties) {
            if (!first) {
                iv.iconst(31);
                iv.mul(Type.INT_TYPE);
            }

            genPropertyOnStack(iv, propertyDescriptor, 0);

            Label ifNull = null;
            Type asmType = typeMapper.mapType(propertyDescriptor.getType());
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

    private void generateDataClassToStringMethod(List<PropertyDescriptor> properties) {
        final MethodVisitor mv = v.getVisitor().visitMethod(ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
        final InstructionAdapter iv = new InstructionAdapter(mv);

        mv.visitCode();
        genStringBuilderConstructor(iv);

        boolean first = true;
        for (PropertyDescriptor propertyDescriptor : properties) {
            if (first) {
                iv.aconst(descriptor.getName() + "(" + propertyDescriptor.getName().getName()+"=");
                first = false;
            }
            else {
                iv.aconst(", " + propertyDescriptor.getName().getName()+"=");
            }
            genInvokeAppendMethod(iv, JAVA_STRING_TYPE);

            Type type = genPropertyOnStack(iv, propertyDescriptor, 0);

            if (type.getSort() == Type.ARRAY) {
                final Type elementType = correctElementType(type);
                if (elementType.getSort() == Type.OBJECT || elementType.getSort() == Type.ARRAY) {
                    iv.invokestatic("java/util/Arrays", "toString", "([Ljava/lang/Object;)Ljava/lang/String;");
                    type = JAVA_STRING_TYPE;
                }
                else {
                    if (elementType.getSort() != Type.CHAR) {
                        iv.invokestatic("java/util/Arrays", "toString", "(" + type.getDescriptor() + ")Ljava/lang/String;");
                        type = JAVA_STRING_TYPE;
                    }
                }
            }
            genInvokeAppendMethod(iv, type);
        }

        iv.aconst(")");
        genInvokeAppendMethod(iv, JAVA_STRING_TYPE);

        iv.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
        iv.areturn(JAVA_STRING_TYPE);

        FunctionCodegen.endVisit(mv, "toString", myClass);
    }

    private Type genPropertyOnStack(InstructionAdapter iv, PropertyDescriptor propertyDescriptor, int index) {
        iv.load(index, classAsmType);
        final Method
                method = typeMapper.mapGetterSignature(propertyDescriptor, OwnerKind.IMPLEMENTATION).getJvmMethodSignature().getAsmMethod();

        iv.invokevirtual(classAsmType.getInternalName(), method.getName(), method.getDescriptor());
        return method.getReturnType();
    }

    private void generateComponentFunctionsForDataClasses() {
        if (!myClass.hasPrimaryConstructor() || !KotlinBuiltIns.getInstance().isData(descriptor)) return;

        ConstructorDescriptor constructor = descriptor.getConstructors().iterator().next();

        for (ValueParameterDescriptor parameter : constructor.getValueParameters()) {
            FunctionDescriptor function = bindingContext.get(BindingContext.DATA_CLASS_COMPONENT_FUNCTION, parameter);
            if (function != null) {
                generateComponentFunction(function, parameter);
            }
        }
    }

    private void generateComponentFunction(@NotNull FunctionDescriptor function, @NotNull ValueParameterDescriptor parameter) {
        JetType returnType = function.getReturnType();
        assert returnType != null : "Return type of component function should not be null: " + function;
        Type componentType = typeMapper.mapReturnType(returnType);

        final String desc = "()" + componentType.getDescriptor();
        MethodVisitor mv = v.newMethod(myClass,
                                       AsmUtil.getMethodAsmFlags(function, OwnerKind.IMPLEMENTATION),
                                       function.getName().getName(),
                                       desc,
                                       null, null);

        FunctionCodegen.genJetAnnotations(state, function, null, null, mv);

        mv.visitCode();
        InstructionAdapter iv = new InstructionAdapter(mv);
        if (!componentType.equals(Type.VOID_TYPE)) {
            iv.load(0, classAsmType);
            iv.invokevirtual(classAsmType.getInternalName(), PropertyCodegen.getterName(parameter.getName()), desc);
        }
        iv.areturn(componentType);

        FunctionCodegen.endVisit(mv, function.getName().getName(), myClass);
    }

    private void generateCopyFunction(@NotNull final FunctionDescriptor function) {
        JetType returnType = function.getReturnType();
        assert returnType != null : "Return type of copy function should not be null: " + function;

        JvmMethodSignature methodSignature = typeMapper.mapSignature(function.getName(), function);
        final String methodDesc = methodSignature.getAsmMethod().getDescriptor();

        MethodVisitor mv = v.newMethod(myClass, AsmUtil.getMethodAsmFlags(function, OwnerKind.IMPLEMENTATION),
                                       function.getName().getName(), methodDesc,
                                       null, null);

        FunctionCodegen.genJetAnnotations(state, function, null, null, mv);

        mv.visitCode();
        InstructionAdapter iv = new InstructionAdapter(mv);

        ConstructorDescriptor constructor = DescriptorUtils.getConstructorOfDataClass(descriptor);


        Label methodBegin = new Label();
        mv.visitLabel(methodBegin);

        final Type thisDescriptorType = typeMapper.mapType(descriptor.getDefaultType());
        iv.anew(thisDescriptorType);
        iv.dup();

        String thisInternalName = thisDescriptorType.getInternalName();

        assert function.getValueParameters().size() == constructor.getValueParameters().size() :
                "Number of parameters of copy function and constructor are different. Copy: " + function.getValueParameters().size() + ", constructor: " + constructor.getValueParameters().size();

        MutableClosure closure = context.closure;
        if (closure != null && closure.getCaptureThis() != null) {
            final Type type = typeMapper.mapType(enclosingClassDescriptor(bindingContext, descriptor));
            iv.load(0, classAsmType);
            iv.getfield(JvmClassName.byType(classAsmType).getInternalName(), CAPTURED_THIS_FIELD, type.getDescriptor());
        }

        int parameterIndex = 1; // localVariable 0 = this
        for (ValueParameterDescriptor parameterDescriptor : function.getValueParameters()) {
            Type type = typeMapper.mapType(parameterDescriptor.getType());
            iv.load(parameterIndex, type);
            parameterIndex += type.getSize();
        }

        String constructorJvmDescriptor = typeMapper.mapToCallableMethod(constructor).getSignature().getAsmMethod().getDescriptor();
        iv.invokespecial(thisInternalName, "<init>", constructorJvmDescriptor);

        iv.areturn(thisDescriptorType);

        Label methodEnd = new Label();
        mv.visitLabel(methodEnd);

        FunctionCodegen.MethodBounds methodBounds = new FunctionCodegen.MethodBounds(methodBegin, methodEnd);
        FunctionCodegen.generateLocalVariableTable(typeMapper, mv, function, thisDescriptorType, FunctionCodegen.generateLocalVariablesInfo(function), methodBounds);

        FunctionCodegen.endVisit(mv, function.getName().getName(), myClass);

        final MethodContext functionContext = context.intoFunction(function);
        FunctionCodegen.generateDefaultIfNeeded(functionContext, state, v, methodSignature.getAsmMethod(), function, OwnerKind.IMPLEMENTATION,
                    new DefaultParameterValueLoader() {
                        @Override
                        public void putValueOnStack(
                                ValueParameterDescriptor descriptor,
                                ExpressionCodegen codegen
                        ) {
                            assert (KotlinBuiltIns.getInstance().isData((ClassDescriptor) function.getContainingDeclaration()))
                                    : "Trying to create function with default arguments for function that isn't presented in code for class without data annotation";
                            PropertyDescriptor propertyDescriptor = codegen.getBindingContext().get(
                                    BindingContext.VALUE_PARAMETER_AS_PROPERTY, descriptor);
                            assert propertyDescriptor != null : "Trying to generate default value for parameter of copy function that doesn't correspond to any property";
                            codegen.v.load(0, thisDescriptorType);
                            Type propertyType = codegen.typeMapper.mapType(propertyDescriptor.getType());
                            codegen.intermediateValueForProperty(propertyDescriptor, false, null).put(propertyType, codegen.v);
                        }
                    });
    }

    private void generateEnumMethods() {
        if (myEnumConstants.size() > 0) {
            {
                Type type =
                        typeMapper.mapType(KotlinBuiltIns.getInstance().getArrayType(descriptor.getDefaultType()),
                                           JetTypeMapperMode.IMPL);

                MethodVisitor mv =
                        v.newMethod(myClass, ACC_PUBLIC | ACC_STATIC, "values", "()" + type.getDescriptor(), null, null);
                mv.visitCode();
                mv.visitFieldInsn(GETSTATIC, typeMapper.mapType(descriptor).getInternalName(),
                                  VALUES,
                                  type.getDescriptor());
                mv.visitMethodInsn(INVOKEVIRTUAL, type.getInternalName(), "clone", "()Ljava/lang/Object;");
                mv.visitTypeInsn(CHECKCAST, type.getInternalName());
                mv.visitInsn(ARETURN);
                FunctionCodegen.endVisit(mv, "values()", myClass);
            }
            {

                MethodVisitor mv =
                        v.newMethod(myClass, ACC_PUBLIC | ACC_STATIC, "valueOf", "(Ljava/lang/String;)" + classAsmType.getDescriptor(), null,
                                    null);
                mv.visitCode();
                mv.visitLdcInsn(classAsmType);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;");
                mv.visitTypeInsn(CHECKCAST, classAsmType.getInternalName());
                mv.visitInsn(ARETURN);
                FunctionCodegen.endVisit(mv, "values()", myClass);
            }
        }
    }

    private void generateAccessors() {
        for (Map.Entry<DeclarationDescriptor, DeclarationDescriptor> entry : context.getAccessors().entrySet()) {
            genAccessor(entry);
        }
    }

    private void genAccessor(Map.Entry<DeclarationDescriptor, DeclarationDescriptor> entry) {
        if (entry.getValue() instanceof FunctionDescriptor) {
            FunctionDescriptor bridge = (FunctionDescriptor) entry.getValue();
            FunctionDescriptor original = (FunctionDescriptor) entry.getKey();

            Method method = typeMapper.mapSignature(bridge.getName(), bridge).getAsmMethod();
            final boolean isConstructor = original instanceof ConstructorDescriptor;
            Method originalMethod = isConstructor ?
                                    typeMapper.mapToCallableMethod((ConstructorDescriptor) original).getSignature().getAsmMethod() :
                                    typeMapper.mapSignature(original.getName(), original).getAsmMethod();
            Type[] argTypes = method.getArgumentTypes();

            String owner = typeMapper.getOwner(original, OwnerKind.IMPLEMENTATION).getInternalName();
            MethodVisitor mv = v.newMethod(null, ACC_BRIDGE | ACC_SYNTHETIC | ACC_STATIC, bridge.getName().getName(),
                                           method.getDescriptor(), null, null);
            if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
                genStubCode(mv);
            }
            else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
                mv.visitCode();

                InstructionAdapter iv = new InstructionAdapter(mv);

                if (isConstructor) {
                    iv.anew(method.getReturnType());
                    iv.dup();
                }
                else {
                    // todo: note that for now we never have access bridges for namespace methods, if at some point we do...
                    iv.load(0, OBJECT_TYPE);
                }

                for (int i = isConstructor ? 0 : 1, reg = isConstructor ? 0 : 1; i < argTypes.length; i++) {
                    Type argType = argTypes[i];
                    iv.load(reg, argType);
                    //noinspection AssignmentToForLoopParameter
                    reg += argType.getSize();
                }
                iv.invokespecial(owner, originalMethod.getName(), originalMethod.getDescriptor());

                iv.areturn(method.getReturnType());
                FunctionCodegen.endVisit(iv, "accessor", null);
            }
        }
        else if (entry.getValue() instanceof PropertyDescriptor) {
            PropertyDescriptor bridge = (PropertyDescriptor) entry.getValue();
            PropertyDescriptor original = (PropertyDescriptor) entry.getKey();

            {
                Method method = typeMapper.mapGetterSignature(bridge, OwnerKind.IMPLEMENTATION).getJvmMethodSignature().getAsmMethod();
                JvmPropertyAccessorSignature originalSignature = typeMapper.mapGetterSignature(original, OwnerKind.IMPLEMENTATION);
                Method originalMethod = originalSignature.getJvmMethodSignature().getAsmMethod();
                MethodVisitor mv =
                        v.newMethod(null, ACC_BRIDGE | ACC_SYNTHETIC | ACC_STATIC, method.getName(), method.getDescriptor(), null, null);
                PropertyGetterDescriptor getter = ((PropertyDescriptor) entry.getValue()).getGetter();
                assert getter != null;
                PropertyCodegen.generateJetPropertyAnnotation(mv, originalSignature.getPropertyTypeKotlinSignature(),
                                                              originalSignature.getJvmMethodSignature().getKotlinTypeParameter(),
                                                              original,
                                                              getter.getVisibility());
                if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
                    genStubCode(mv);
                }
                else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
                    mv.visitCode();

                    InstructionAdapter iv = new InstructionAdapter(mv);

                    iv.load(0, OBJECT_TYPE);
                    boolean hasBackingField = Boolean.TRUE.equals(bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, original));
                    if (original.getVisibility() == Visibilities.PRIVATE && hasBackingField) {
                        iv.getfield(typeMapper.getOwner(original, OwnerKind.IMPLEMENTATION).getInternalName(), original.getName().getName(),
                                    originalMethod.getReturnType().getDescriptor());
                    }
                    else {
                        iv.invokespecial(typeMapper.getOwner(original, OwnerKind.IMPLEMENTATION).getInternalName(),
                                         originalMethod.getName(), originalMethod.getDescriptor());
                    }

                    iv.areturn(method.getReturnType());
                    FunctionCodegen.endVisit(iv, "accessor", null);
                }
            }

            if (bridge.isVar()) {
                Method method = typeMapper.mapSetterSignature(bridge, OwnerKind.IMPLEMENTATION).getJvmMethodSignature().getAsmMethod();
                JvmPropertyAccessorSignature originalSignature2 = typeMapper.mapSetterSignature(original, OwnerKind.IMPLEMENTATION);
                Method originalMethod = originalSignature2.getJvmMethodSignature().getAsmMethod();
                MethodVisitor mv =
                        v.newMethod(null, ACC_STATIC | ACC_BRIDGE | ACC_FINAL, method.getName(), method.getDescriptor(), null, null);
                PropertySetterDescriptor setter = ((PropertyDescriptor) entry.getValue()).getSetter();
                assert setter != null;
                PropertyCodegen.generateJetPropertyAnnotation(mv, originalSignature2.getPropertyTypeKotlinSignature(),
                                                              originalSignature2.getJvmMethodSignature().getKotlinTypeParameter(),
                                                              original,
                                                              setter.getVisibility());
                if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
                    genStubCode(mv);
                }
                else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
                    mv.visitCode();

                    InstructionAdapter iv = new InstructionAdapter(mv);

                    iv.load(0, OBJECT_TYPE);
                    Type[] argTypes = method.getArgumentTypes();
                    for (int i = 1, reg = 1; i < argTypes.length; i++) {
                        Type argType = argTypes[i];
                        iv.load(reg, argType);
                        //noinspection AssignmentToForLoopParameter
                        reg += argType.getSize();
                    }
                    if (original.getVisibility() == Visibilities.PRIVATE && original.getModality() == Modality.FINAL) {
                        iv.putfield(typeMapper.getOwner(original, OwnerKind.IMPLEMENTATION).getInternalName(), original.getName().getName(),
                                    originalMethod.getArgumentTypes()[0].getDescriptor());
                    }
                    else {
                        iv.invokespecial(typeMapper.getOwner(original, OwnerKind.IMPLEMENTATION).getInternalName(),
                                         originalMethod.getName(), originalMethod.getDescriptor());
                    }

                    iv.areturn(method.getReturnType());
                    FunctionCodegen.endVisit(iv, "accessor", null);
                }
            }
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    private void generateFieldForSingleton() {
        if (!(isNonLiteralObject(myClass) || descriptor.getKind() == ClassKind.CLASS_OBJECT)) return;

        v.newField(myClass, ACC_PUBLIC | ACC_STATIC | ACC_FINAL, JvmAbi.INSTANCE_FIELD, classAsmType.getDescriptor(), null, null);

        staticInitializerChunks.add(new CodeChunk() {
            @Override
            public void generate(InstructionAdapter iv) {
                genInitSingletonField(classAsmType, iv);
            }
        });
    }

    protected void generatePrimaryConstructor() {
        if (ignoreIfTraitOrAnnotation()) return;

        if (kind != OwnerKind.IMPLEMENTATION) {
            throw new IllegalStateException("incorrect kind for primary constructor: " + kind);
        }

        ConstructorDescriptor constructorDescriptor = bindingContext.get(BindingContext.CONSTRUCTOR, myClass);

        final ConstructorContext constructorContext = context.intoConstructor(constructorDescriptor);

        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            lookupConstructorExpressionsInClosureIfPresent(constructorContext);
        }

        MutableClosure closure = context.closure;
        boolean hasCapturedThis = closure != null && closure.getCaptureThis() != null;

        final CallableMethod callableMethod = typeMapper.mapToCallableMethod(constructorDescriptor, context.closure);
        final JvmMethodSignature constructorMethod = callableMethod.getSignature();

        assert constructorDescriptor != null;
        int flags = getConstructorAsmFlags(constructorDescriptor);
        final MethodVisitor mv = v.newMethod(myClass, flags, constructorMethod.getName(), constructorMethod.getAsmMethod().getDescriptor(),
                                             constructorMethod.getGenericsSignature(), null);
        if (state.getClassBuilderMode() != ClassBuilderMode.SIGNATURES) {

            AnnotationVisitor jetConstructorVisitor = mv.visitAnnotation(JvmStdlibNames.JET_CONSTRUCTOR.getDescriptor(), true);

            int flagsValue = getFlagsForVisibility(constructorDescriptor.getVisibility());
            if (JvmStdlibNames.FLAGS_DEFAULT_VALUE != flagsValue) {
                jetConstructorVisitor.visit(JvmStdlibNames.JET_FLAGS_FIELD, flagsValue);
            }

            jetConstructorVisitor.visitEnd();

            AnnotationCodegen.forMethod(mv, typeMapper).genAnnotations(constructorDescriptor);

            writeParameterAnnotations(constructorDescriptor, constructorMethod, hasCapturedThis, mv);

            if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
                genStubCode(mv);
                return;
            }

            generatePrimaryConstructorImpl(constructorDescriptor, constructorContext, constructorMethod, callableMethod, hasCapturedThis,
                                           closure, mv);
        }

        FunctionCodegen.generateConstructorWithoutParametersIfNeeded(state, callableMethod, constructorDescriptor, v);
    }

    private void generatePrimaryConstructorImpl(
            ConstructorDescriptor constructorDescriptor,
            ConstructorContext constructorContext,
            JvmMethodSignature constructorMethod,
            CallableMethod callableMethod,
            boolean hasCapturedThis,
            MutableClosure closure,
            MethodVisitor mv
    ) {
        mv.visitCode();

        List<ValueParameterDescriptor> paramDescrs = constructorDescriptor != null
                                                     ? constructorDescriptor.getValueParameters()
                                                     : Collections.<ValueParameterDescriptor>emptyList();

        ConstructorFrameMap frameMap = new ConstructorFrameMap(callableMethod, constructorDescriptor);

        final InstructionAdapter iv = new InstructionAdapter(mv);
        ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, constructorContext, state);

        JvmClassName classname = JvmClassName.byType(classAsmType);

        if (superCall == null) {
            genSimpleSuperCall(iv);
        }
        else if (superCall instanceof JetDelegatorToSuperClass) {
            genSuperCallToDelegatorToSuperClass(iv);
        }
        else {
            generateDelegatorToConstructorCall(iv, codegen, constructorDescriptor, frameMap);
        }

        if (hasCapturedThis) {
            final Type type = typeMapper
                    .mapType(enclosingClassDescriptor(bindingContext, descriptor));
            String interfaceDesc = type.getDescriptor();
            iv.load(0, classAsmType);
            iv.load(frameMap.getOuterThisIndex(), type);
            iv.putfield(classname.getInternalName(), CAPTURED_THIS_FIELD, interfaceDesc);
        }

        if (closure != null) {
            int k = hasCapturedThis ? 2 : 1;
            final String internalName = typeMapper.mapType(descriptor).getInternalName();
            final ClassifierDescriptor captureReceiver = closure.getCaptureReceiver();
            if (captureReceiver != null) {
                iv.load(0, OBJECT_TYPE);
                final Type asmType = typeMapper.mapType(captureReceiver.getDefaultType(), JetTypeMapperMode.IMPL);
                iv.load(k, asmType);
                iv.putfield(internalName, CAPTURED_RECEIVER_FIELD, asmType.getDescriptor());
                k += asmType.getSize();
            }

            for (DeclarationDescriptor varDescr : closure.getCaptureVariables().keySet()) {
                if (varDescr instanceof VariableDescriptor && !(varDescr instanceof PropertyDescriptor)) {
                    Type sharedVarType = typeMapper.getSharedVarType(varDescr);
                    if (sharedVarType == null) {
                        sharedVarType = typeMapper.mapType((VariableDescriptor) varDescr);
                    }
                    iv.load(0, OBJECT_TYPE);
                    iv.load(k, StackValue.refType(sharedVarType));
                    k += StackValue.refType(sharedVarType).getSize();
                    iv.putfield(internalName,
                                "$" + varDescr.getName(), sharedVarType.getDescriptor());
                }
            }
        }

        int n = 0;
        for (JetDelegationSpecifier specifier : myClass.getDelegationSpecifiers()) {
            if (specifier == superCall) {
                continue;
            }

            if (specifier instanceof JetDelegatorByExpressionSpecifier) {
                genCallToDelegatorByExpressionSpecifier(iv, codegen, classAsmType, classname, n++, specifier);
            }
        }

        int curParam = 0;
        List<JetParameter> constructorParameters = getPrimaryConstructorParameters();
        for (JetParameter parameter : constructorParameters) {
            if (parameter.getValOrVarNode() != null) {
                VariableDescriptor descriptor = paramDescrs.get(curParam);
                Type type = typeMapper.mapType(descriptor);
                iv.load(0, classAsmType);
                iv.load(frameMap.getIndex(descriptor), type);
                iv.putfield(classAsmType.getInternalName(), descriptor.getName().getName(), type.getDescriptor());
            }
            curParam++;
        }

        generateInitializers(codegen, iv, myClass.getDeclarations(), bindingContext, state);

        mv.visitInsn(RETURN);
        FunctionCodegen.endVisit(mv, "constructor", myClass);

        assert constructorDescriptor != null;
        FunctionCodegen.generateDefaultIfNeeded(constructorContext, state, v, constructorMethod.getAsmMethod(), constructorDescriptor,
                    OwnerKind.IMPLEMENTATION, DefaultParameterValueLoader.DEFAULT);
    }

    private void genSuperCallToDelegatorToSuperClass(InstructionAdapter iv) {
        iv.load(0, superClassAsmType);
        JetType superType = bindingContext.get(BindingContext.TYPE, superCall.getTypeReference());
        List<Type> parameterTypes = new ArrayList<Type>();
        assert superType != null;
        ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
        if (CodegenBinding.hasThis0(bindingContext, superClassDescriptor)) {
            iv.load(1, OBJECT_TYPE);
            parameterTypes.add(typeMapper.mapType(
                    enclosingClassDescriptor(bindingContext, descriptor)));
        }
        Method superCallMethod = new Method("<init>", Type.VOID_TYPE, parameterTypes.toArray(new Type[parameterTypes.size()]));
        //noinspection ConstantConditions
        iv.invokespecial(typeMapper.mapType(superClassDescriptor).getInternalName(), "<init>",
                         superCallMethod.getDescriptor());
    }

    private void genSimpleSuperCall(InstructionAdapter iv) {
        iv.load(0, superClassAsmType);
        if (descriptor.getKind() == ClassKind.ENUM_CLASS || descriptor.getKind() == ClassKind.ENUM_ENTRY) {
            iv.load(1, JAVA_STRING_TYPE);
            iv.load(2, Type.INT_TYPE);
            iv.invokespecial(superClassAsmType.getInternalName(), "<init>", "(Ljava/lang/String;I)V");
        }
        else {
            iv.invokespecial(superClassAsmType.getInternalName(), "<init>", "()V");
        }
    }

    private void writeParameterAnnotations(
            ConstructorDescriptor constructorDescriptor,
            JvmMethodSignature constructorMethod,
            boolean hasThis0,
            MethodVisitor mv
    ) {
        if (constructorDescriptor != null) {
            int i = 0;

            if (hasThis0) {
                i++;
            }

            if (descriptor.getKind() == ClassKind.ENUM_CLASS || descriptor.getKind() == ClassKind.ENUM_ENTRY) {
                i += 2;
            }

            for (ValueParameterDescriptor valueParameter : constructorDescriptor.getValueParameters()) {
                AnnotationCodegen.forParameter(i, mv, state.getTypeMapper()).genAnnotations(valueParameter);
                JetValueParameterAnnotationWriter jetValueParameterAnnotation =
                        JetValueParameterAnnotationWriter.visitParameterAnnotation(mv, i);
                jetValueParameterAnnotation.writeName(valueParameter.getName().getName());
                jetValueParameterAnnotation.writeHasDefaultValue(valueParameter.declaresDefaultValue());
                jetValueParameterAnnotation.writeVararg(valueParameter.getVarargElementType() != null);
                jetValueParameterAnnotation.writeType(constructorMethod.getKotlinParameterType(i));
                jetValueParameterAnnotation.visitEnd();
                ++i;
            }
        }
    }

    private void genCallToDelegatorByExpressionSpecifier(
            InstructionAdapter iv,
            ExpressionCodegen codegen,
            Type classType,
            JvmClassName classname,
            int n,
            JetDelegationSpecifier specifier
    ) {
        final JetExpression expression = ((JetDelegatorByExpressionSpecifier) specifier).getDelegateExpression();
        PropertyDescriptor propertyDescriptor = null;
        if (expression instanceof JetSimpleNameExpression) {
            final ResolvedCall<? extends CallableDescriptor> call = bindingContext.get(BindingContext.RESOLVED_CALL, expression);
            if (call != null) {
                final CallableDescriptor callResultingDescriptor = call.getResultingDescriptor();
                if (callResultingDescriptor instanceof ValueParameterDescriptor) {
                    final ValueParameterDescriptor valueParameterDescriptor = (ValueParameterDescriptor) callResultingDescriptor;
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

        JetType superType = bindingContext.get(BindingContext.TYPE, specifier.getTypeReference());
        assert superType != null;

        ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
        assert superClassDescriptor != null;

        final Type superTypeAsmType = typeMapper.mapType(superType, JetTypeMapperMode.IMPL);

        StackValue field;
        if (propertyDescriptor != null &&
            !propertyDescriptor.isVar() &&
            Boolean.TRUE.equals(bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor))) {
            // final property with backing field
            field = StackValue.field(typeMapper.mapType(propertyDescriptor.getType()), classname,
                                     propertyDescriptor.getName().getName(), false);
        }
        else {
            iv.load(0, classType);
            codegen.genToJVMStack(expression);

            String delegateField = "$delegate_" + n;
            Type fieldType = typeMapper.mapType(superClassDescriptor);
            String fieldDesc = fieldType.getDescriptor();

            v.newField(specifier, ACC_PRIVATE|ACC_FINAL|ACC_SYNTHETIC, delegateField, fieldDesc, /*TODO*/null, null);

            field = StackValue.field(fieldType, classname, delegateField, false);
            field.store(fieldType, iv);
        }

        generateDelegates(superClassDescriptor, field);
    }

    private void lookupConstructorExpressionsInClosureIfPresent(final ConstructorContext constructorContext) {
        final JetVisitorVoid visitor = new JetVisitorVoid() {
            @Override
            public void visitJetElement(JetElement e) {
                e.acceptChildren(this);
            }

            @Override
            public void visitSimpleNameExpression(JetSimpleNameExpression expr) {
                final DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expr);
                if (descriptor instanceof VariableDescriptor && !(descriptor instanceof PropertyDescriptor)) {
                    ConstructorDescriptor constructorDescriptor = (ConstructorDescriptor) constructorContext.getContextDescriptor();
                    for (ValueParameterDescriptor parameterDescriptor : constructorDescriptor.getValueParameters()) {
                        //noinspection ConstantConditions
                        if (descriptor.equals(parameterDescriptor)) {
                            return;
                        }
                    }
                    constructorContext.lookupInContext(descriptor, null, state, true);
                }
            }

            @Override
            public void visitThisExpression(JetThisExpression expression) {
                final DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getInstanceReference());
                if (descriptor instanceof ClassDescriptor) {
                    // @todo for now all our classes are inner so no need to lookup this. change it when we have real inners
                }
                else {
                    assert descriptor instanceof CallableDescriptor;
                    if (context.getCallableDescriptorWithReceiver() != descriptor) {
                        context.lookupInContext(descriptor, null, state, false);
                    }
                }
            }
        };

        for (JetDeclaration declaration : myClass.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                final JetProperty property = (JetProperty) declaration;
                final JetExpression initializer = property.getInitializer();
                if (initializer != null) {
                    initializer.accept(visitor);
                }
            }
            else if (declaration instanceof JetClassInitializer) {
                final JetClassInitializer initializer = (JetClassInitializer) declaration;
                initializer.accept(visitor);
            }
        }

        for (JetDelegationSpecifier specifier : myClass.getDelegationSpecifiers()) {
            if (specifier != superCall) {
                if (specifier instanceof JetDelegatorByExpressionSpecifier) {
                    JetExpression delegateExpression = ((JetDelegatorByExpressionSpecifier) specifier).getDelegateExpression();
                    assert delegateExpression != null;
                    delegateExpression.accept(visitor);
                }
            }
            else {
                if (superCall instanceof JetDelegatorToSuperCall) {
                    final JetValueArgumentList argumentList = ((JetDelegatorToSuperCall) superCall).getValueArgumentList();
                    if (argumentList != null) {
                        argumentList.accept(visitor);
                    }
                }
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
        if (myClass instanceof JetClass &&
            (((JetClass) myClass).isTrait() || myClass.hasModifier(JetTokens.ABSTRACT_KEYWORD))) {
            return;
        }

        for (Pair<CallableMemberDescriptor, CallableMemberDescriptor> needDelegates : getTraitImplementations(descriptor)) {
            if (needDelegates.second instanceof SimpleFunctionDescriptor) {
                generateDelegationToTraitImpl((FunctionDescriptor) needDelegates.second, (FunctionDescriptor) needDelegates.first);
            }
            else if (needDelegates.second instanceof PropertyDescriptor) {
                PropertyDescriptor property = (PropertyDescriptor) needDelegates.second;
                List<PropertyAccessorDescriptor> inheritedAccessors = ((PropertyDescriptor) needDelegates.first).getAccessors();
                for (PropertyAccessorDescriptor accessor : property.getAccessors()) {
                    for (PropertyAccessorDescriptor inheritedAccessor : inheritedAccessors) {
                        if (inheritedAccessor.getClass() == accessor.getClass()) { // same accessor kind
                            generateDelegationToTraitImpl(accessor, inheritedAccessor);
                        }
                    }
                }
            }
        }
    }


    private void generateDelegationToTraitImpl(FunctionDescriptor fun, @NotNull FunctionDescriptor inheritedFun) {
        DeclarationDescriptor containingDeclaration = fun.getContainingDeclaration();
        if (containingDeclaration instanceof ClassDescriptor) {
            ClassDescriptor declaration = (ClassDescriptor) containingDeclaration;
            if (declaration.getKind() == ClassKind.TRAIT) {
                int flags = ACC_PUBLIC; // TODO.

                Method function;
                Method functionOriginal;
                if (fun instanceof PropertyAccessorDescriptor) {
                    PropertyDescriptor property = ((PropertyAccessorDescriptor) fun).getCorrespondingProperty();
                    if (fun instanceof PropertyGetterDescriptor) {
                        function = typeMapper.mapGetterSignature(property, OwnerKind.IMPLEMENTATION).getJvmMethodSignature().getAsmMethod();
                        functionOriginal =
                                typeMapper.mapGetterSignature(property.getOriginal(), OwnerKind.IMPLEMENTATION).getJvmMethodSignature()
                                        .getAsmMethod();
                    }
                    else if (fun instanceof PropertySetterDescriptor) {
                        function = typeMapper.mapSetterSignature(property, OwnerKind.IMPLEMENTATION).getJvmMethodSignature().getAsmMethod();
                        functionOriginal =
                                typeMapper.mapSetterSignature(property.getOriginal(), OwnerKind.IMPLEMENTATION).getJvmMethodSignature()
                                        .getAsmMethod();
                    }
                    else {
                        throw new IllegalStateException("Accessor is neither getter, nor setter, what is it?");
                    }
                }
                else {
                    function = typeMapper.mapSignature(fun.getName(), fun).getAsmMethod();
                    functionOriginal = typeMapper.mapSignature(fun.getName(), fun.getOriginal()).getAsmMethod();
                }

                final MethodVisitor mv = v.newMethod(myClass, flags, function.getName(), function.getDescriptor(), null, null);
                AnnotationCodegen.forMethod(mv, state.getTypeMapper()).genAnnotations(fun);

                JvmMethodSignature jvmSignature = typeMapper.mapToCallableMethod(
                        inheritedFun,
                        false,
                        isCallInsideSameClassAsDeclared(inheritedFun, context),
                        OwnerKind.IMPLEMENTATION).getSignature();
                JetMethodAnnotationWriter aw = JetMethodAnnotationWriter.visitAnnotation(mv);
                int kotlinFlags = getFlagsForVisibility(fun.getVisibility());
                if (fun instanceof PropertyAccessorDescriptor) {
                    kotlinFlags |= JvmStdlibNames.FLAG_PROPERTY_BIT;
                    aw.writeTypeParameters(jvmSignature.getKotlinTypeParameter());
                    aw.writePropertyType(jvmSignature.getKotlinReturnType());
                }
                else {
                    JetType returnType = fun.getReturnType();
                    assert returnType != null;
                    aw.writeTypeParameters(jvmSignature.getKotlinTypeParameter());
                    aw.writeReturnType(jvmSignature.getKotlinReturnType());
                }
                kotlinFlags |= DescriptorKindUtils.kindToFlags(inheritedFun.getKind());
                aw.writeFlags(kotlinFlags);
                aw.visitEnd();

                if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
                    genStubCode(mv);
                }
                else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
                    mv.visitCode();
                    FrameMap frameMap = context.prepareFrame(state.getTypeMapper());
                    ExpressionCodegen codegen =
                            new ExpressionCodegen(mv, frameMap, jvmSignature.getAsmMethod().getReturnType(), context, state);
                    codegen.generateThisOrOuter(descriptor, false);    // ??? wouldn't it be addClosureToConstructorParameters good idea to put it?

                    Type[] argTypes = function.getArgumentTypes();
                    List<Type> originalArgTypes = jvmSignature.getValueParameterTypes();

                    InstructionAdapter iv = new InstructionAdapter(mv);
                    iv.load(0, OBJECT_TYPE);
                    for (int i = 0, reg = 1; i < argTypes.length; i++) {
                        StackValue.local(reg, argTypes[i]).put(originalArgTypes.get(i), iv);
                        //noinspection AssignmentToForLoopParameter
                        reg += argTypes[i].getSize();
                    }

                    JetType jetType = CodegenUtil.getSuperClass(declaration);
                    Type type = typeMapper.mapType(jetType, JetTypeMapperMode.IMPL);
                    if (type.getInternalName().equals("java/lang/Object")) {
                        jetType = declaration.getDefaultType();
                        type = typeMapper.mapType(jetType, JetTypeMapperMode.IMPL);
                    }

                    String fdescriptor = functionOriginal.getDescriptor().replace("(", "(" + type.getDescriptor());
                    Type type1 =
                            typeMapper.mapType(((ClassDescriptor) fun.getContainingDeclaration()).getDefaultType(), JetTypeMapperMode.TRAIT_IMPL);
                    iv.invokestatic(type1.getInternalName(), function.getName(), fdescriptor);
                    if (function.getReturnType().getSort() == Type.OBJECT &&
                        !function.getReturnType().equals(functionOriginal.getReturnType())) {
                        iv.checkcast(function.getReturnType());
                    }
                    iv.areturn(function.getReturnType());
                    FunctionCodegen.endVisit(iv, "trait method", callableDescriptorToDeclaration(bindingContext, fun));
                }

                FunctionCodegen.generateBridgeIfNeeded(context, state, v, function, fun);
            }
        }
    }

    private void generateDelegatorToConstructorCall(
            InstructionAdapter iv, ExpressionCodegen codegen,
            ConstructorDescriptor constructorDescriptor,
            ConstructorFrameMap frameMap
    ) {
        ClassDescriptor classDecl = constructorDescriptor.getContainingDeclaration();

        iv.load(0, OBJECT_TYPE);

        if (classDecl.getKind() == ClassKind.ENUM_CLASS || classDecl.getKind() == ClassKind.ENUM_ENTRY) {
            iv.load(1, OBJECT_TYPE);
            iv.load(2, Type.INT_TYPE);
        }

        CallableMethod method = typeMapper.mapToCallableMethod(constructorDescriptor, context.closure);

        final ResolvedCall<? extends CallableDescriptor> resolvedCall =
                bindingContext.get(BindingContext.RESOLVED_CALL, ((JetCallElement) superCall).getCalleeExpression());
        assert resolvedCall != null;
        final ConstructorDescriptor superConstructor = (ConstructorDescriptor) resolvedCall.getResultingDescriptor();

        //noinspection SuspiciousMethodCalls
        final CalculatedClosure closureForSuper = bindingContext.get(CLOSURE, superConstructor.getContainingDeclaration());
        CallableMethod superCallable = typeMapper.mapToCallableMethod(superConstructor, closureForSuper);

        if (closureForSuper != null && closureForSuper.getCaptureThis() != null) {
            iv.load(frameMap.getOuterThisIndex(), OBJECT_TYPE);
        }

        if (myClass instanceof JetObjectDeclaration &&
            superCall instanceof JetDelegatorToSuperCall &&
            ((JetObjectDeclaration) myClass).isObjectLiteral()) {
            int nextVar = findFirstSuperArgument(method);
            for (Type t : superCallable.getSignature().getAsmMethod().getArgumentTypes()) {
                iv.load(nextVar, t);
                nextVar += t.getSize();
            }
            superCallable.invokeWithNotNullAssertion(codegen.v, state, resolvedCall);
        }
        else {
            codegen.invokeMethodWithArguments(superCallable, (JetCallElement) superCall, StackValue.none());
        }
    }

    private static int findFirstSuperArgument(CallableMethod method) {
        final List<JvmMethodParameterSignature> types = method.getSignature().getKotlinParameterTypes();
        if (types != null) {
            int i = 0;
            for (JvmMethodParameterSignature type : types) {
                if (type.getKind() == JvmMethodParameterKind.SUPER_CALL_PARAM) {
                    return i + 1; // because of this
                }
                i += type.getAsmType().getSize();
            }
        }
        return -1;
    }

    @Override
    protected void generateDeclaration(PropertyCodegen propertyCodegen, JetDeclaration declaration, FunctionCodegen functionCodegen) {
        if (declaration instanceof JetClassObject || declaration instanceof JetObjectDeclaration) {
            // done earlier in order to have accessors
        }
        else if (declaration instanceof JetEnumEntry) {
            String name = declaration.getName();
            final String desc = "L" + classAsmType.getInternalName() + ";";
            v.newField(declaration, ACC_PUBLIC | ACC_ENUM | ACC_STATIC | ACC_FINAL, name, desc, null, null);
            if (myEnumConstants.isEmpty()) {
                staticInitializerChunks.add(new CodeChunk() {
                    @Override
                    public void generate(InstructionAdapter v) {
                        initializeEnumConstants(v);
                    }
                });
            }
            myEnumConstants.add((JetEnumEntry) declaration);
        }
        else {
            super.generateDeclaration(propertyCodegen, declaration, functionCodegen);
        }
    }

    private final List<JetEnumEntry> myEnumConstants = new ArrayList<JetEnumEntry>();

    private void initializeEnumConstants(InstructionAdapter iv) {
        ExpressionCodegen codegen = new ExpressionCodegen(iv, new FrameMap(), Type.VOID_TYPE, context, state);
        int ordinal = -1;
        JetType myType = descriptor.getDefaultType();
        Type myAsmType = typeMapper.mapType(myType, JetTypeMapperMode.IMPL);

        assert myEnumConstants.size() > 0;
        JetType arrayType = KotlinBuiltIns.getInstance().getArrayType(myType);
        Type arrayAsmType = typeMapper.mapType(arrayType, JetTypeMapperMode.IMPL);
        v.newField(myClass, ACC_PRIVATE | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC, "$VALUES", arrayAsmType.getDescriptor(), null, null);

        iv.iconst(myEnumConstants.size());
        iv.newarray(myAsmType);
        iv.dup();

        for (JetEnumEntry enumConstant : myEnumConstants) {
            ordinal++;

            iv.dup();
            iv.iconst(ordinal);

            ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, enumConstant);
            assert classDescriptor != null;
            String implClass = typeMapper.mapType(classDescriptor.getDefaultType(), JetTypeMapperMode.IMPL).getInternalName();

            final List<JetDelegationSpecifier> delegationSpecifiers = enumConstant.getDelegationSpecifiers();
            if (delegationSpecifiers.size() > 1) {
                throw new UnsupportedOperationException("multiple delegation specifiers for enum constant not supported");
            }

            iv.anew(Type.getObjectType(implClass));
            iv.dup();

            iv.aconst(enumConstant.getName());
            iv.iconst(ordinal);

            if (delegationSpecifiers.size() == 1 && !enumEntryNeedSubclass(state.getBindingContext(), enumConstant)) {
                final JetDelegationSpecifier specifier = delegationSpecifiers.get(0);
                if (specifier instanceof JetDelegatorToSuperCall) {
                    final JetDelegatorToSuperCall superCall = (JetDelegatorToSuperCall) specifier;
                    ConstructorDescriptor constructorDescriptor = (ConstructorDescriptor) bindingContext
                            .get(BindingContext.REFERENCE_TARGET, superCall.getCalleeExpression().getConstructorReferenceExpression());
                    assert constructorDescriptor != null;
                    //noinspection SuspiciousMethodCalls
                    CallableMethod method = typeMapper.mapToCallableMethod(constructorDescriptor);
                    codegen.invokeMethodWithArguments(method, superCall, StackValue.none());
                }
                else {
                    throw new UnsupportedOperationException("unsupported type of enum constant initializer: " + specifier);
                }
            }
            else {
                iv.invokespecial(implClass, "<init>", "(Ljava/lang/String;I)V");
            }
            iv.dup();
            iv.putstatic(myAsmType.getInternalName(), enumConstant.getName(), "L" + myAsmType.getInternalName() + ";");
            iv.astore(OBJECT_TYPE);
        }
        iv.putstatic(myAsmType.getInternalName(), "$VALUES", arrayAsmType.getDescriptor());
    }

    public static void generateInitializers(
            @NotNull ExpressionCodegen codegen, @NotNull InstructionAdapter iv, @NotNull List<JetDeclaration> declarations,
            @NotNull BindingContext bindingContext, @NotNull GenerationState state
    ) {
        JetTypeMapper typeMapper = state.getTypeMapper();
        for (JetDeclaration declaration : declarations) {
            if (declaration instanceof JetProperty) {
                final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) bindingContext.get(BindingContext.VARIABLE, declaration);
                assert propertyDescriptor != null;
                if (Boolean.TRUE.equals(bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor))) {
                    final JetExpression initializer = ((JetProperty) declaration).getInitializer();
                    if (initializer != null) {
                        CompileTimeConstant<?> compileTimeValue = bindingContext.get(BindingContext.COMPILE_TIME_VALUE, initializer);
                        final JetType jetType = propertyDescriptor.getType();
                        if (compileTimeValue != null) {
                            Object value = compileTimeValue.getValue();
                            Type type = typeMapper.mapType(jetType);
                            if (skipDefaultValue(propertyDescriptor, value, type)) continue;
                        }
                        iv.load(0, OBJECT_TYPE);
                        Type type = codegen.expressionType(initializer);
                        if (jetType.isNullable()) {
                            type = boxType(type);
                        }
                        codegen.gen(initializer, type);
                        // @todo write directly to the field. Fix test excloset.jet::test6
                        JvmClassName owner = typeMapper.getOwner(propertyDescriptor, OwnerKind.IMPLEMENTATION);
                        Type propType = typeMapper.mapType(jetType);
                        StackValue.property(propertyDescriptor, owner, owner,
                                            propType, false, false, false, null, null, 0, 0, state).store(propType, iv);
                    }
                }
            }
            else if (declaration instanceof JetClassInitializer) {
                codegen.gen(((JetClassInitializer) declaration).getBody(), Type.VOID_TYPE);
            }
        }
    }

    private static boolean skipDefaultValue(PropertyDescriptor propertyDescriptor, Object value, Type type) {
        if (isPrimitive(type)) {
            if (!propertyDescriptor.getType().isNullable() && value instanceof Number) {
                if (type == Type.INT_TYPE && ((Number) value).intValue() == 0) {
                    return true;
                }
                if (type == Type.BYTE_TYPE && ((Number) value).byteValue() == 0) {
                    return true;
                }
                if (type == Type.LONG_TYPE && ((Number) value).longValue() == 0L) {
                    return true;
                }
                if (type == Type.SHORT_TYPE && ((Number) value).shortValue() == 0) {
                    return true;
                }
                if (type == Type.DOUBLE_TYPE && ((Number) value).doubleValue() == 0d) {
                    return true;
                }
                if (type == Type.FLOAT_TYPE && ((Number) value).byteValue() == 0f) {
                    return true;
                }
            }
            if (type == Type.BOOLEAN_TYPE && value instanceof Boolean && !((Boolean) value)) {
                return true;
            }
            if (type == Type.CHAR_TYPE && value instanceof Character && ((Character) value) == 0) {
                return true;
            }
        }
        else {
            if (value == null) {
                return true;
            }
        }
        return false;
    }

    protected void generateDelegates(ClassDescriptor toClass, StackValue field) {
        final FunctionCodegen functionCodegen = new FunctionCodegen(context, v, state);
        final PropertyCodegen propertyCodegen = new PropertyCodegen(context, v, functionCodegen);

        for (DeclarationDescriptor declaration : descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            if (declaration instanceof CallableMemberDescriptor) {
                CallableMemberDescriptor callableMemberDescriptor = (CallableMemberDescriptor) declaration;
                if (callableMemberDescriptor.getKind() == CallableMemberDescriptor.Kind.DELEGATION) {
                    Set<? extends CallableMemberDescriptor> overriddenDescriptors = callableMemberDescriptor.getOverriddenDescriptors();
                    for (CallableMemberDescriptor overriddenDescriptor : overriddenDescriptors) {
                        if (overriddenDescriptor.getContainingDeclaration() == toClass) {
                            if (declaration instanceof PropertyDescriptor) {
                                propertyCodegen
                                        .genDelegate((PropertyDescriptor) declaration, (PropertyDescriptor) overriddenDescriptor, field);
                            }
                            else if (declaration instanceof FunctionDescriptor) {
                                functionCodegen
                                        .genDelegate((FunctionDescriptor) declaration, (FunctionDescriptor) overriddenDescriptor, field);
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * Return pairs of descriptors. First is member of this that should be implemented by delegating to trait,
     * second is member of trait that contain implementation.
     */
    private static List<Pair<CallableMemberDescriptor, CallableMemberDescriptor>> getTraitImplementations(@NotNull ClassDescriptor classDescriptor) {
        List<Pair<CallableMemberDescriptor, CallableMemberDescriptor>> r = Lists.newArrayList();

        root:
        for (DeclarationDescriptor decl : classDescriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            if (!(decl instanceof CallableMemberDescriptor)) {
                continue;
            }

            CallableMemberDescriptor callableMemberDescriptor = (CallableMemberDescriptor) decl;
            if (callableMemberDescriptor.getKind() != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                continue;
            }

            Collection<CallableMemberDescriptor> overriddenDeclarations =
                    OverridingUtil.getOverriddenDeclarations(callableMemberDescriptor);
            for (CallableMemberDescriptor overriddenDeclaration : overriddenDeclarations) {
                if (overriddenDeclaration.getModality() != Modality.ABSTRACT) {
                    if (!isInterface(overriddenDeclaration.getContainingDeclaration())) {
                        continue root;
                    }
                }
            }

            for (CallableMemberDescriptor overriddenDeclaration : overriddenDeclarations) {
                if (overriddenDeclaration.getModality() != Modality.ABSTRACT) {
                    r.add(Pair.create(callableMemberDescriptor, overriddenDeclaration));
                }
            }
        }

        return r;
    }
}
