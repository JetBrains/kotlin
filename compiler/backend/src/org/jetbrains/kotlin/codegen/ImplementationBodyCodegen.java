/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import kotlin.KotlinPackage;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.backend.common.CodegenUtilKt;
import org.jetbrains.kotlin.backend.common.DataClassMethodGenerator;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.codegen.binding.MutableClosure;
import org.jetbrains.kotlin.codegen.context.*;
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension;
import org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil;
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.java.JvmAnnotationNames;
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinClass;
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.annotations.AnnotationsPackage;
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.CallResolverUtilPackage;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmClassSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.resolve.scopes.LookupLocation;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver;
import org.jetbrains.kotlin.serialization.*;
import org.jetbrains.kotlin.serialization.deserialization.NameResolver;
import org.jetbrains.kotlin.serialization.jvm.BitEncoding;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.*;

import static kotlin.KotlinPackage.firstOrNull;
import static org.jetbrains.kotlin.codegen.AsmUtil.*;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.*;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.enumEntryNeedSubclass;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.getDelegationConstructorCall;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.getNotNull;
import static org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.descriptorToDeclaration;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage.getResolvedCall;
import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage.getBuiltIns;
import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage.getSecondaryConstructors;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.*;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.DiagnosticsPackage.*;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.NO_ORIGIN;
import static org.jetbrains.kotlin.types.Variance.INVARIANT;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class ImplementationBodyCodegen extends ClassBodyCodegen {
    private static final String ENUM_VALUES_FIELD_NAME = "$VALUES";
    private Type superClassAsmType;
    @Nullable // null means java/lang/Object
    private JetType superClassType;
    private final Type classAsmType;

    private List<PropertyAndDefaultValue> companionObjectPropertiesToCopy;

    private final List<Function2<ImplementationBodyCodegen, ClassBuilder, Unit>> additionalTasks =
            new ArrayList<Function2<ImplementationBodyCodegen, ClassBuilder, Unit>>();

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
            if (jetClass.hasModifier(JetTokens.ABSTRACT_KEYWORD) || jetClass.isSealed()) {
                isAbstract = true;
            }
            if (jetClass.isInterface()) {
                isAbstract = true;
                isInterface = true;
            }
            else if (descriptor.getKind() == ClassKind.ANNOTATION_CLASS) {
                isAbstract = true;
                isInterface = true;
                isAnnotation = true;
            }
            else if (jetClass.isEnum()) {
                isAbstract = hasAbstractMembers(descriptor);
                isEnum = true;
            }

            if (isObject(descriptor)) {
                isFinal = true;
            }

            if (!jetClass.hasModifier(JetTokens.OPEN_KEYWORD) && !isAbstract) {
                // Light-class mode: Do not make enum classes final since PsiClass corresponding to enum is expected to be inheritable from
                isFinal = !(jetClass.isEnum() && state.getClassBuilderMode() == ClassBuilderMode.LIGHT_CLASSES);
            }
            isStatic = !jetClass.isInner();
        }
        else {
            isStatic = isCompanionObject(descriptor);
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
        if (KotlinBuiltIns.isDeprecated(descriptor)) {
            access |= ACC_DEPRECATED;
        }
        if (isEnum) {
            for (JetDeclaration declaration : myClass.getDeclarations()) {
                if (declaration instanceof JetEnumEntry) {
                    if (enumEntryNeedSubclass(bindingContext, (JetEnumEntry) declaration)) {
                        access &= ~ACC_FINAL;
                    }
                }
            }
            access |= ACC_ENUM;
        }

        v.defineClass(
                myClass, V1_6,
                access,
                signature.getName(),
                signature.getJavaGenericSignature(),
                signature.getSuperclassName(),
                ArrayUtil.toStringArray(signature.getInterfaces())
        );

        v.visitSource(myClass.getContainingFile().getName(), null);

        InlineCodegenUtil.initDefaultSourceMappingIfNeeded(context, this, state);

        writeEnclosingMethod();

        AnnotationCodegen.forClass(v.getVisitor(), typeMapper).genAnnotations(descriptor, null);

        generateReflectionObjectFieldIfNeeded();

        generateEnumEntries();
    }

    @Override
    protected void generateKotlinAnnotation() {
        if (state.getClassBuilderMode() != ClassBuilderMode.FULL) return;

        KotlinClass.Kind kind;
        if (isAnonymousObject(descriptor)) {
            kind = KotlinClass.Kind.ANONYMOUS_OBJECT;
        }
        else if (isTopLevelOrInnerClass(descriptor)) {
            // Default value is Kind.CLASS
            kind = null;
        }
        else {
            // LOCAL_CLASS is also written to inner classes of local classes
            kind = KotlinClass.Kind.LOCAL_CLASS;
        }

        // Temporarily write class kind anyway because old compiler may not expect its absence
        // TODO: remove after M13
        if (kind == null) {
            kind = KotlinClass.Kind.CLASS;
        }

        DescriptorSerializer serializer =
                DescriptorSerializer.create(descriptor, new JvmSerializerExtension(v.getSerializationBindings(), typeMapper));

        ProtoBuf.Class classProto = serializer.classProto(descriptor).build();

        StringTable strings = serializer.getStringTable();
        NameResolver nameResolver = new NameResolver(strings.serializeSimpleNames(), strings.serializeQualifiedNames());
        ClassData data = new ClassData(nameResolver, classProto);

        AnnotationVisitor av = v.getVisitor().visitAnnotation(asmDescByFqNameWithoutInnerClasses(JvmAnnotationNames.KOTLIN_CLASS), true);
        av.visit(JvmAnnotationNames.ABI_VERSION_FIELD_NAME, JvmAbi.VERSION);
        //noinspection ConstantConditions
        if (kind != null) {
            av.visitEnum(
                    JvmAnnotationNames.KIND_FIELD_NAME,
                    Type.getObjectType(KotlinClass.KIND_INTERNAL_NAME).getDescriptor(),
                    kind.toString()
            );
        }
        AnnotationVisitor array = av.visitArray(JvmAnnotationNames.DATA_FIELD_NAME);
        for (String string : BitEncoding.encodeBytes(SerializationUtil.serializeClassData(data))) {
            array.visit(null, string);
        }
        array.visitEnd();
        av.visitEnd();
    }

    private void writeEnclosingMethod() {
        // Do not emit enclosing method in "light-classes mode" since currently we generate local light classes as if they're top level
        if (state.getClassBuilderMode() == ClassBuilderMode.LIGHT_CLASSES) {
            return;
        }

        //JVMS7: A class must have an EnclosingMethod attribute if and only if it is a local class or an anonymous class.
        if (isAnonymousObject(descriptor) || !(descriptor.getContainingDeclaration() instanceof ClassOrPackageFragmentDescriptor)) {
            writeOuterClassAndEnclosingMethod();
        }
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

        LinkedHashSet<String> superInterfaces = new LinkedHashSet<String>();

        for (JetType supertype : descriptor.getTypeConstructor().getSupertypes()) {
            if (isInterface(supertype.getConstructor().getDeclarationDescriptor())) {
                sw.writeInterface();
                Type jvmName = typeMapper.mapSupertype(supertype, sw);
                sw.writeInterfaceEnd();
                superInterfaces.add(jvmName.getInternalName());
            }
        }

        return new JvmClassSignature(classAsmType.getInternalName(), superClassAsmType.getInternalName(),
                                     new ArrayList<String>(superInterfaces), sw.makeJavaGenericSignature());
    }

    protected void getSuperClass() {
        superClassAsmType = OBJECT_TYPE;
        superClassType = null;

        if (descriptor.getKind() == ClassKind.INTERFACE) {
            return;
        }

        for (JetType supertype : descriptor.getTypeConstructor().getSupertypes()) {
            ClassifierDescriptor superClass = supertype.getConstructor().getDeclarationDescriptor();
            if (superClass != null && !isInterface(superClass)) {
                superClassAsmType = typeMapper.mapClass(superClass);
                superClassType = supertype;
                return;
            }
        }
    }

    @Override
    protected void generateSyntheticParts() {
        generatePropertyMetadataArrayFieldIfNeeded(classAsmType);

        generateFieldForSingleton();

        generateCompanionObjectBackingFieldCopies();

        DelegationFieldsInfo delegationFieldsInfo = getDelegationFieldsInfo(myClass.getDelegationSpecifiers());
        try {
            lookupConstructorExpressionsInClosureIfPresent();
            generatePrimaryConstructor(delegationFieldsInfo);
            for (ConstructorDescriptor secondaryConstructor : getSecondaryConstructors(descriptor)) {
                generateSecondaryConstructor(secondaryConstructor);
            }
        }
        catch (CompilationException e) {
            throw e;
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (RuntimeException e) {
            throw new RuntimeException("Error generating constructors of class " + myClass.getName() + " with kind " + kind, e);
        }

        generateTraitMethods();

        generateDelegates(delegationFieldsInfo);

        generateSyntheticAccessors();

        generateEnumMethods();

        generateFunctionsForDataClasses();

        new CollectionStubMethodGenerator(state, descriptor, functionCodegen, v).generate();

        generateToArray();

        genClosureFields(context.closure, v, typeMapper);

        for (ExpressionCodegenExtension extension : ExpressionCodegenExtension.Companion.getInstances(state.getProject())) {
            extension.generateClassSyntheticParts(v, state, myClass, descriptor);
        }
    }

    private void generateReflectionObjectFieldIfNeeded() {
        if (isAnnotationClass(descriptor)) {
            // There's a bug in JDK 6 and 7 that prevents us from generating a static field in an annotation class:
            // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6857918
            // TODO: make reflection work on annotation classes somehow
            return;
        }

        generateReflectionObjectField(state, classAsmType, v, method("createKotlinClass", K_CLASS_TYPE, getType(Class.class)),
                                      JvmAbi.KOTLIN_CLASS_FIELD_NAME, createOrGetClInitCodegen().v);
    }

    private boolean isGenericToArrayPresent() {
        Collection<FunctionDescriptor> functions =
                descriptor.getDefaultType().getMemberScope().getFunctions(Name.identifier("toArray"), LookupLocation.NO_LOCATION);
        for (FunctionDescriptor function : functions) {
            if (CallResolverUtilPackage.isOrOverridesSynthesized(function)) {
                continue;
            }

            if (function.getValueParameters().size() != 1 || function.getTypeParameters().size() != 1) {
                continue;
            }

            JetType returnType = function.getReturnType();
            assert returnType != null : function.toString();
            JetType paramType = function.getValueParameters().get(0).getType();
            if (KotlinBuiltIns.isArray(returnType) && KotlinBuiltIns.isArray(paramType)) {
                JetType elementType = function.getTypeParameters().get(0).getDefaultType();
                if (JetTypeChecker.DEFAULT.equalTypes(elementType, getBuiltIns(descriptor).getArrayElementType(returnType))
                        && JetTypeChecker.DEFAULT.equalTypes(elementType, getBuiltIns(descriptor).getArrayElementType(paramType))) {
                    return true;
                }
            }
        }
        return false;

    }

    private void generateToArray() {
        KotlinBuiltIns builtIns = getBuiltIns(descriptor);
        if (!isSubclass(descriptor, builtIns.getCollection())) return;

        int access = descriptor.getKind() == ClassKind.INTERFACE ?
                     ACC_PUBLIC | ACC_ABSTRACT :
                     ACC_PUBLIC;
        if (CodegenUtil.getDeclaredFunctionByRawSignature(descriptor, Name.identifier("toArray"), builtIns.getArray()) == null) {
            MethodVisitor mv = v.newMethod(NO_ORIGIN, access, "toArray", "()[Ljava/lang/Object;", null, null);

            if (descriptor.getKind() != ClassKind.INTERFACE) {
                InstructionAdapter iv = new InstructionAdapter(mv);
                mv.visitCode();

                iv.load(0, classAsmType);
                iv.invokestatic("kotlin/jvm/internal/CollectionToArray", "toArray", "(Ljava/util/Collection;)[Ljava/lang/Object;", false);
                iv.areturn(Type.getType("[Ljava/lang/Object;"));

                FunctionCodegen.endVisit(mv, "toArray", myClass);
            }
        }

        if (!isGenericToArrayPresent()) {
            MethodVisitor mv = v.newMethod(NO_ORIGIN, access, "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", null, null);

            if (descriptor.getKind() != ClassKind.INTERFACE) {
                InstructionAdapter iv = new InstructionAdapter(mv);
                mv.visitCode();

                iv.load(0, classAsmType);
                iv.load(1, Type.getType("[Ljava/lang/Object;"));

                iv.invokestatic("kotlin/jvm/internal/CollectionToArray", "toArray",
                                "(Ljava/util/Collection;[Ljava/lang/Object;)[Ljava/lang/Object;", false);
                iv.areturn(Type.getType("[Ljava/lang/Object;"));

                FunctionCodegen.endVisit(mv, "toArray", myClass);
            }
        }
    }

    private void generateFunctionsForDataClasses() {
        if (!KotlinBuiltIns.isData(descriptor)) return;

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
            KotlinBuiltIns builtins = getBuiltIns(descriptor);
            FunctionDescriptor equalsFunction = CodegenUtil.getDeclaredFunctionByRawSignature(
                    descriptor, Name.identifier(CodegenUtil.EQUALS_METHOD_NAME), builtins.getBoolean(), builtins.getAny()
            );

            assert equalsFunction != null : String.format("Should be called only for classes with non-trivial '%s'. In %s, %s",
                                                          CodegenUtil.EQUALS_METHOD_NAME, descriptor.getName(), descriptor);

            MethodContext context = ImplementationBodyCodegen.this.context.intoFunction(equalsFunction);
            MethodVisitor mv = v.newMethod(OtherOrigin(equalsFunction), ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
            InstructionAdapter iv = new InstructionAdapter(mv);

            mv.visitCode();
            Label eq = new Label();
            Label ne = new Label();

            iv.load(0, OBJECT_TYPE);
            iv.load(1, OBJECT_TYPE);
            iv.ifacmpeq(eq);

            iv.load(1, OBJECT_TYPE);
            iv.instanceOf(classAsmType);
            iv.ifeq(ne);

            iv.load(1, OBJECT_TYPE);
            iv.checkcast(classAsmType);
            iv.store(2, OBJECT_TYPE);

            for (PropertyDescriptor propertyDescriptor : properties) {
                Type asmType = typeMapper.mapType(propertyDescriptor);

                Type thisPropertyType = genPropertyOnStack(iv, context, propertyDescriptor, 0);
                StackValue.coerce(thisPropertyType, asmType, iv);

                Type otherPropertyType = genPropertyOnStack(iv, context, propertyDescriptor, 2);
                StackValue.coerce(otherPropertyType, asmType, iv);

                if (asmType.getSort() == Type.ARRAY) {
                    Type elementType = correctElementType(asmType);
                    if (elementType.getSort() == Type.OBJECT || elementType.getSort() == Type.ARRAY) {
                        iv.invokestatic("java/util/Arrays", "equals", "([Ljava/lang/Object;[Ljava/lang/Object;)Z", false);
                    }
                    else {
                        iv.invokestatic("java/util/Arrays", "equals",
                                        "(" + asmType.getDescriptor() + asmType.getDescriptor() + ")Z", false);
                    }
                    iv.ifeq(ne);
                }
                else if (asmType.getSort() == Type.FLOAT) {
                    iv.invokestatic("java/lang/Float", "compare", "(FF)I", false);
                    iv.ifne(ne);
                }
                else if (asmType.getSort() == Type.DOUBLE) {
                    iv.invokestatic("java/lang/Double", "compare", "(DD)I", false);
                    iv.ifne(ne);
                }
                else {
                    StackValue value = genEqualsForExpressionsOnStack(JetTokens.EQEQ, StackValue.onStack(asmType), StackValue.onStack(asmType));
                    value.put(Type.BOOLEAN_TYPE, iv);
                    iv.ifeq(ne);
                }
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
                    descriptor, Name.identifier(CodegenUtil.HASH_CODE_METHOD_NAME), getBuiltIns(descriptor).getInt()
            );

            assert hashCodeFunction != null : String.format("Should be called only for classes with non-trivial '%s'. In %s, %s",
                                                            CodegenUtil.HASH_CODE_METHOD_NAME, descriptor.getName(), descriptor);

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

                Type propertyType = genPropertyOnStack(iv, context, propertyDescriptor, 0);
                Type asmType = typeMapper.mapType(propertyDescriptor);
                StackValue.coerce(propertyType, asmType, iv);

                Label ifNull = null;
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
                    descriptor, Name.identifier(CodegenUtil.TO_STRING_METHOD_NAME), getBuiltIns(descriptor).getString()
            );

            assert toString != null : String.format("Should be called only for classes with non-trivial '%s'. In %s, %s",
                                                    CodegenUtil.TO_STRING_METHOD_NAME, descriptor.getName(), descriptor);

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

        private Type genPropertyOnStack(InstructionAdapter iv, MethodContext context, @NotNull PropertyDescriptor propertyDescriptor, int index) {
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
            PsiElement originalElement = DescriptorToSourceUtils.descriptorToDeclaration(parameter);
            functionCodegen.generateMethod(OtherOrigin(originalElement, function), function, new FunctionGenerationStrategy() {
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
                        assert property != null : "Property descriptor is not found for primary constructor parameter: " + parameter;

                        Type propertyType = genPropertyOnStack(iv, context, property, 0);
                        StackValue.coerce(propertyType, componentType, iv);
                    }
                    iv.areturn(componentType);
                }
            });
        }

        @Override
        public void generateCopyFunction(@NotNull final FunctionDescriptor function, @NotNull List<JetParameter> constructorParameters) {
            final Type thisDescriptorType = typeMapper.mapType(descriptor);

            functionCodegen.generateMethod(OtherOrigin(myClass, function), function, new FunctionGenerationStrategy() {
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

                    ConstructorDescriptor constructor = getPrimaryConstructorOfDataClass(descriptor);
                    assert function.getValueParameters().size() == constructor.getValueParameters().size() :
                            "Number of parameters of copy function and constructor are different. " +
                            "Copy: " + function.getValueParameters().size() + ", " +
                            "constructor: " + constructor.getValueParameters().size();

                    MutableClosure closure = ImplementationBodyCodegen.this.context.closure;
                    if (closure != null) {
                        pushCapturedFieldsOnStack(iv, closure);
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

                private void pushCapturedFieldsOnStack(InstructionAdapter iv, MutableClosure closure) {
                    ClassDescriptor captureThis = closure.getCaptureThis();
                    if (captureThis != null) {
                        iv.load(0, classAsmType);
                        Type type = typeMapper.mapType(captureThis);
                        iv.getfield(classAsmType.getInternalName(), CAPTURED_THIS_FIELD, type.getDescriptor());
                    }

                    JetType captureReceiver = closure.getCaptureReceiverType();
                    if (captureReceiver != null) {
                        iv.load(0, classAsmType);
                        Type type = typeMapper.mapType(captureReceiver);
                        iv.getfield(classAsmType.getInternalName(), CAPTURED_RECEIVER_FIELD, type.getDescriptor());
                    }

                    for (Map.Entry<DeclarationDescriptor, EnclosedValueDescriptor> entry : closure.getCaptureVariables().entrySet()) {
                        DeclarationDescriptor declarationDescriptor = entry.getKey();
                        EnclosedValueDescriptor enclosedValueDescriptor = entry.getValue();
                        StackValue capturedValue = enclosedValueDescriptor.getInstanceValue();
                        Type sharedVarType = typeMapper.getSharedVarType(declarationDescriptor);
                        if (sharedVarType == null) {
                            sharedVarType = typeMapper.mapType((VariableDescriptor) declarationDescriptor);
                        }
                        capturedValue.put(sharedVarType, iv);
                    }
                }
            });

            functionCodegen.generateDefaultIfNeeded(
                    context.intoFunction(function), function, OwnerKind.IMPLEMENTATION,
                    new DefaultParameterValueLoader() {
                        @Override
                        public StackValue genValue(ValueParameterDescriptor valueParameter, ExpressionCodegen codegen) {
                            assert KotlinBuiltIns.isData((ClassDescriptor) function.getContainingDeclaration())
                                    : "Function container should be annotated with [data]: " + function;
                            PropertyDescriptor property = bindingContext.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, valueParameter);
                            assert property != null : "Copy function doesn't correspond to any property: " + function;
                            return codegen.intermediateValueForProperty(property, false, null, StackValue.LOCAL_0);
                        }
                    },
                    null
            );
        }
    }

    @NotNull
    private static ConstructorDescriptor getPrimaryConstructorOfDataClass(@NotNull ClassDescriptor classDescriptor) {
        ConstructorDescriptor constructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
        assert constructor != null : "Data class must have primary constructor: " + classDescriptor;
        return constructor;
    }

    private void generateEnumMethods() {
        if (isEnumClass(descriptor)) {
            generateEnumValuesMethod();
            generateEnumValueOfMethod();
        }
    }

    private void generateEnumValuesMethod() {
        Type type = typeMapper.mapType(getBuiltIns(descriptor).getArrayType(INVARIANT, descriptor.getDefaultType()));

        FunctionDescriptor valuesFunction =
                KotlinPackage.single(descriptor.getStaticScope().getFunctions(ENUM_VALUES, LookupLocation.NO_LOCATION), new Function1<FunctionDescriptor, Boolean>() {
                    @Override
                    public Boolean invoke(FunctionDescriptor descriptor) {
                        return CodegenUtil.isEnumValuesMethod(descriptor);
                    }
                });
        MethodVisitor mv = v.newMethod(OtherOrigin(myClass, valuesFunction), ACC_PUBLIC | ACC_STATIC, ENUM_VALUES.asString(),
                                       "()" + type.getDescriptor(), null, null);
        if (state.getClassBuilderMode() != ClassBuilderMode.FULL) return;

        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, classAsmType.getInternalName(), ENUM_VALUES_FIELD_NAME, type.getDescriptor());
        mv.visitMethodInsn(INVOKEVIRTUAL, type.getInternalName(), "clone", "()Ljava/lang/Object;", false);
        mv.visitTypeInsn(CHECKCAST, type.getInternalName());
        mv.visitInsn(ARETURN);
        FunctionCodegen.endVisit(mv, "values()", myClass);
    }

    private void generateEnumValueOfMethod() {
        FunctionDescriptor valueOfFunction =
                KotlinPackage.single(descriptor.getStaticScope().getFunctions(ENUM_VALUE_OF, LookupLocation.NO_LOCATION), new Function1<FunctionDescriptor, Boolean>() {
                    @Override
                    public Boolean invoke(FunctionDescriptor descriptor) {
                        return CodegenUtil.isEnumValueOfMethod(descriptor);
                    }
                });
        MethodVisitor mv = v.newMethod(OtherOrigin(myClass, valueOfFunction), ACC_PUBLIC | ACC_STATIC, ENUM_VALUE_OF.asString(),
                                       "(Ljava/lang/String;)" + classAsmType.getDescriptor(), null, null);
        if (state.getClassBuilderMode() != ClassBuilderMode.FULL) return;

        mv.visitCode();
        mv.visitLdcInsn(classAsmType);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;", false);
        mv.visitTypeInsn(CHECKCAST, classAsmType.getInternalName());
        mv.visitInsn(ARETURN);
        FunctionCodegen.endVisit(mv, "valueOf()", myClass);
    }

    protected void generateSyntheticAccessors() {
        Map<DeclarationDescriptor, DeclarationDescriptor> accessors = ((CodegenContext<?>) context).getAccessors();
        for (Map.Entry<DeclarationDescriptor, DeclarationDescriptor> entry : accessors.entrySet()) {
            generateSyntheticAccessor(entry);
        }
    }

    private void generateSyntheticAccessor(Map.Entry<DeclarationDescriptor, DeclarationDescriptor> entry) {
        if (entry.getValue() instanceof FunctionDescriptor) {
            final FunctionDescriptor bridge = (FunctionDescriptor) entry.getValue();
            final FunctionDescriptor original = (FunctionDescriptor) entry.getKey();
            functionCodegen.generateMethod(
                    Synthetic(null, original), bridge,
                    new FunctionGenerationStrategy.CodegenBased<FunctionDescriptor>(state, bridge) {
                        @Override
                        public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                            markLineNumberForSyntheticFunction(descriptor, codegen.v);

                            generateMethodCallTo(original, bridge, codegen.v);
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
                                         !isCompanionObject(bridge.getContainingDeclaration());
                    StackValue property =
                            codegen.intermediateValueForProperty(original, forceField, null, MethodKind.SYNTHETIC_ACCESSOR,
                                                                 StackValue.none());

                    InstructionAdapter iv = codegen.v;

                    markLineNumberForSyntheticFunction(descriptor, iv);

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
                        property.store(StackValue.onStack(property.type), iv, true);
                    }

                    iv.areturn(signature.getReturnType());
                }
            }

            PropertyGetterDescriptor getter = bridge.getGetter();
            assert getter != null;
            functionCodegen.generateMethod(Synthetic(null, original.getGetter() != null ? original.getGetter() : original),
                                           getter, new PropertyAccessorStrategy(getter));


            if (bridge.isVar()) {
                PropertySetterDescriptor setter = bridge.getSetter();
                assert setter != null;

                functionCodegen.generateMethod(Synthetic(null, original.getSetter() != null ? original.getSetter() : original),
                                               setter, new PropertyAccessorStrategy(setter));
            }
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    public static void markLineNumberForSyntheticFunction(@Nullable ClassDescriptor declarationDescriptor, @NotNull InstructionAdapter v) {
        if (declarationDescriptor == null) {
            return;
        }

        PsiElement classElement = DescriptorToSourceUtils.getSourceFromDescriptor(declarationDescriptor);
        if (classElement != null) {
            Integer lineNumber = CodegenUtil.getLineNumberForElement(classElement, false);
            if (lineNumber != null) {
                Label label = new Label();
                v.visitLabel(label);
                v.visitLineNumber(lineNumber, label);
            }
        }
    }

    private void generateMethodCallTo(
            @NotNull FunctionDescriptor functionDescriptor,
            @Nullable FunctionDescriptor bridgeDescriptor,
            @NotNull InstructionAdapter iv
    ) {
        boolean isConstructor = functionDescriptor instanceof ConstructorDescriptor;
        boolean bridgeIsAccessorConstructor = bridgeDescriptor instanceof AccessorForConstructorDescriptor;
        boolean callFromAccessor = bridgeIsAccessorConstructor
                                   || (bridgeDescriptor != null && JetTypeMapper.isAccessor(bridgeDescriptor));
        CallableMethod callableMethod = isConstructor ?
                                        typeMapper.mapToCallableMethod((ConstructorDescriptor) functionDescriptor) :
                                        typeMapper.mapToCallableMethod(functionDescriptor, callFromAccessor, context);

        int reg = 1;
        if (isConstructor && !bridgeIsAccessorConstructor) {
            iv.anew(callableMethod.getOwner());
            iv.dup();
            reg = 0;
        }
        else if (callFromAccessor) {
            if (!AnnotationsPackage.isPlatformStaticInObjectOrClass(functionDescriptor)) {
                iv.load(0, OBJECT_TYPE);
            }
        }

        for (Type argType : callableMethod.getParameterTypes()) {
            if (AsmTypes.DEFAULT_CONSTRUCTOR_MARKER.equals(argType)) {
                iv.aconst(null);
            }
            else {
                iv.load(reg, argType);
                reg += argType.getSize();
            }
        }
        callableMethod.genInvokeInstruction(iv);
    }

    private void generateFieldForSingleton() {
        if (isEnumEntry(descriptor) || isCompanionObject(descriptor)) return;

        if (isNonCompanionObject(descriptor)) {
            StackValue.Field field = StackValue.singleton(descriptor, typeMapper);
            v.newField(OtherOrigin(myClass), ACC_PUBLIC | ACC_STATIC | ACC_FINAL, field.name, field.type.getDescriptor(), null, null);

            if (state.getClassBuilderMode() != ClassBuilderMode.FULL) return;

            // Invoke the object constructor but ignore the result because INSTANCE$ will be initialized in the first line of <init>
            InstructionAdapter v = createOrGetClInitCodegen().v;
            v.anew(classAsmType);
            v.invokespecial(classAsmType.getInternalName(), "<init>", "()V", false);
            return;
        }

        ClassDescriptor companionObjectDescriptor = descriptor.getCompanionObjectDescriptor();
        if (companionObjectDescriptor == null) {
            return;
        }

        JetObjectDeclaration companionObject = firstOrNull(((JetClass) myClass).getCompanionObjects());
        assert companionObject != null : "Companion object not found: " + myClass.getText();

        StackValue.Field field = StackValue.singleton(companionObjectDescriptor, typeMapper);
        v.newField(OtherOrigin(companionObject), ACC_PUBLIC | ACC_STATIC | ACC_FINAL, field.name, field.type.getDescriptor(), null, null);

        if (state.getClassBuilderMode() != ClassBuilderMode.FULL) return;

        if (!isCompanionObjectWithBackingFieldsInOuter(companionObjectDescriptor)) {
            generateCompanionObjectInitializer(companionObjectDescriptor);
        }
    }

    private void generateCompanionObjectBackingFieldCopies() {
        if (companionObjectPropertiesToCopy == null) return;

        for (PropertyAndDefaultValue info : companionObjectPropertiesToCopy) {
            PropertyDescriptor property = info.descriptor;

            Type type = typeMapper.mapType(property);
            FieldVisitor fv = v.newField(Synthetic(DescriptorToSourceUtils.descriptorToDeclaration(property), property),
                                         ACC_STATIC | ACC_FINAL | ACC_PUBLIC, context.getFieldName(property, false),
                                         type.getDescriptor(), typeMapper.mapFieldSignature(property.getType()),
                                         info.defaultValue);

            AnnotationCodegen.forField(fv, typeMapper).genAnnotations(property, type);

            //This field are always static and final so if it has constant initializer don't do anything in clinit,
            //field would be initialized via default value in v.newField(...) - see JVM SPEC Ch.4
            // TODO: test this code
            if (state.getClassBuilderMode() == ClassBuilderMode.FULL && info.defaultValue == null) {
                ExpressionCodegen codegen = createOrGetClInitCodegen();
                int companionObjectIndex = putCompanionObjectInLocalVar(codegen);
                StackValue.local(companionObjectIndex, OBJECT_TYPE).put(OBJECT_TYPE, codegen.v);
                copyFieldFromCompanionObject(property);
            }
        }
    }

    private int putCompanionObjectInLocalVar(ExpressionCodegen codegen) {
        FrameMap frameMap = codegen.myFrameMap;
        ClassDescriptor companionObjectDescriptor = descriptor.getCompanionObjectDescriptor();
        int companionObjectIndex = frameMap.getIndex(companionObjectDescriptor);
        if (companionObjectIndex == -1) {
            companionObjectIndex = frameMap.enter(companionObjectDescriptor, OBJECT_TYPE);
            StackValue companionObject = StackValue.singleton(companionObjectDescriptor, typeMapper);
            StackValue.local(companionObjectIndex, companionObject.type).store(companionObject, codegen.v);
        }
        return companionObjectIndex;
    }

    private void copyFieldFromCompanionObject(PropertyDescriptor propertyDescriptor) {
        ExpressionCodegen codegen = createOrGetClInitCodegen();
        StackValue property = codegen.intermediateValueForProperty(propertyDescriptor, false, null, StackValue.none());
        StackValue.Field field = StackValue
                .field(property.type, classAsmType, propertyDescriptor.getName().asString(), true, StackValue.none(), propertyDescriptor);
        field.store(property, codegen.v);
    }

    private void generateCompanionObjectInitializer(@NotNull ClassDescriptor companionObject) {
        ExpressionCodegen codegen = createOrGetClInitCodegen();
        FunctionDescriptor constructor = context.accessibleFunctionDescriptor(KotlinPackage.single(companionObject.getConstructors()));
        generateMethodCallTo(constructor, null, codegen.v);
        codegen.v.dup();
        StackValue instance = StackValue.onStack(typeMapper.mapClass(companionObject));
        StackValue.singleton(companionObject, typeMapper).store(instance, codegen.v, true);
    }

    private void generatePrimaryConstructor(final DelegationFieldsInfo delegationFieldsInfo) {
        if (isTrait(descriptor) || isAnnotationClass(descriptor)) return;

        ConstructorDescriptor constructorDescriptor = descriptor.getUnsubstitutedPrimaryConstructor();
        if (constructorDescriptor == null) return;

        ConstructorContext constructorContext = context.intoConstructor(constructorDescriptor);

        JetPrimaryConstructor primaryConstructor = myClass.getPrimaryConstructor();
        JvmDeclarationOrigin origin = OtherOrigin(primaryConstructor != null ? primaryConstructor : myClass, constructorDescriptor);
        functionCodegen.generateMethod(origin, constructorDescriptor, constructorContext,
                   new FunctionGenerationStrategy.CodegenBased<ConstructorDescriptor>(state, constructorDescriptor) {
                       @Override
                       public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                           generatePrimaryConstructorImpl(callableDescriptor, codegen, delegationFieldsInfo);
                       }
                   }
        );

        functionCodegen.generateDefaultIfNeeded(constructorContext, constructorDescriptor, OwnerKind.IMPLEMENTATION,
                                                DefaultParameterValueLoader.DEFAULT, null);

        new DefaultParameterValueSubstitutor(state).generateConstructorOverloadsIfNeeded(constructorDescriptor, v,
                                                                                         constructorContext, myClass);

        if (isCompanionObject(descriptor)) {
            context.recordSyntheticAccessorIfNeeded(constructorDescriptor, bindingContext);
        }
    }

    private void generateSecondaryConstructor(@NotNull ConstructorDescriptor constructorDescriptor) {
        if (!canHaveDeclaredConstructors(descriptor)) return;

        ConstructorContext constructorContext = context.intoConstructor(constructorDescriptor);

        functionCodegen.generateMethod(OtherOrigin(descriptorToDeclaration(constructorDescriptor), constructorDescriptor),
                                       constructorDescriptor, constructorContext,
                                       new FunctionGenerationStrategy.CodegenBased<ConstructorDescriptor>(state, constructorDescriptor) {
                                           @Override
                                           public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                                               generateSecondaryConstructorImpl(callableDescriptor, codegen);
                                           }
                                       }
        );

        functionCodegen.generateDefaultIfNeeded(constructorContext, constructorDescriptor, OwnerKind.IMPLEMENTATION,
                                                DefaultParameterValueLoader.DEFAULT, null);

        new DefaultParameterValueSubstitutor(state).generateOverloadsIfNeeded(myClass, constructorDescriptor, constructorDescriptor,
                                                                              constructorContext, v);
    }

    private void generatePrimaryConstructorImpl(
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull ExpressionCodegen codegen,
            @NotNull DelegationFieldsInfo fieldsInfo
    ) {
        InstructionAdapter iv = codegen.v;

        generateClosureInitialization(iv);

        generateDelegatorToConstructorCall(iv, codegen, constructorDescriptor,
                                           getDelegationConstructorCall(bindingContext, constructorDescriptor));

        if (isNonCompanionObject(descriptor)) {
            StackValue.singleton(descriptor, typeMapper).store(StackValue.LOCAL_0, iv);
        }

        for (JetDelegationSpecifier specifier : myClass.getDelegationSpecifiers()) {
            if (specifier instanceof JetDelegatorByExpressionSpecifier) {
                genCallToDelegatorByExpressionSpecifier(iv, codegen, (JetDelegatorByExpressionSpecifier) specifier, fieldsInfo);
            }
        }

        int curParam = 0;
        List<ValueParameterDescriptor> parameters = constructorDescriptor.getValueParameters();
        for (JetParameter parameter : getPrimaryConstructorParameters()) {
            if (parameter.hasValOrVar()) {
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

        if (isCompanionObjectWithBackingFieldsInOuter(descriptor)) {
            final ImplementationBodyCodegen parentCodegen = (ImplementationBodyCodegen) getParentCodegen();
            parentCodegen.generateCompanionObjectInitializer(descriptor);
            generateInitializers(new Function0<ExpressionCodegen>() {
                @Override
                public ExpressionCodegen invoke() {
                    return parentCodegen.createOrGetClInitCodegen();
                }
            });
        }
        else {
            generateInitializers(codegen);
        }

        iv.visitInsn(RETURN);
    }

    private void generateSecondaryConstructorImpl(
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull ExpressionCodegen codegen
    ) {
        InstructionAdapter iv = codegen.v;

        ResolvedCall<ConstructorDescriptor> constructorDelegationCall =
                getDelegationConstructorCall(bindingContext, constructorDescriptor);
        ConstructorDescriptor delegateConstructor = constructorDelegationCall == null ? null :
                                                     constructorDelegationCall.getResultingDescriptor();

        generateDelegatorToConstructorCall(iv, codegen, constructorDescriptor, constructorDelegationCall);
        if (!isSameClassConstructor(delegateConstructor)) {
            // Initialization happens only for constructors delegating to super
            generateClosureInitialization(iv);
            generateInitializers(codegen);
        }

        JetSecondaryConstructor constructor =
                (JetSecondaryConstructor) DescriptorToSourceUtils.descriptorToDeclaration(constructorDescriptor);
        assert constructor != null;
        if (constructor.hasBody()) {
            codegen.gen(constructor.getBodyExpression(), Type.VOID_TYPE);
        }

        iv.visitInsn(RETURN);
    }

    private void generateInitializers(@NotNull final ExpressionCodegen codegen) {
        generateInitializers(new Function0<ExpressionCodegen>() {
            @Override
            public ExpressionCodegen invoke() {
                return codegen;
            }
        });
    }

    private void generateClosureInitialization(@NotNull InstructionAdapter iv) {
        MutableClosure closure = context.closure;
        if (closure != null) {
            List<FieldInfo> argsFromClosure = ClosureCodegen.calculateConstructorParameters(typeMapper, closure, classAsmType);
            int k = 1;
            for (FieldInfo info : argsFromClosure) {
                k = AsmUtil.genAssignInstanceFieldFromParam(info, k, iv);
            }
        }
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
                return StackValue.field(type, classAsmType, name, false, StackValue.none());
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
                PropertyDescriptor propertyDescriptor = CodegenUtil.getDelegatePropertyIfAny(expression, descriptor, bindingContext);


                if (CodegenUtil.isFinalPropertyWithBackingField(propertyDescriptor, bindingContext)) {
                    result.addField((JetDelegatorByExpressionSpecifier) specifier, propertyDescriptor);
                }
                else {
                    JetType expressionType = expression != null ? bindingContext.getType(expression) : null;
                    Type asmType =
                            expressionType != null ? typeMapper.mapType(expressionType) : typeMapper.mapType(getSuperClass(specifier));
                    result.addField((JetDelegatorByExpressionSpecifier) specifier, asmType, "$delegate_" + n);
                }
                n++;
            }
        }
        return result;
    }

    @NotNull
    private ClassDescriptor getSuperClass(@NotNull JetDelegationSpecifier specifier) {
        return CodegenUtil.getSuperClassByDelegationSpecifier(specifier, bindingContext);
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
            fieldInfo.getStackValue().store(codegen.gen(expression), iv);
        }
    }

    private void lookupConstructorExpressionsInClosureIfPresent() {
        if (state.getClassBuilderMode() != ClassBuilderMode.FULL || descriptor.getConstructors().isEmpty()) return;

        JetVisitorVoid visitor = new JetVisitorVoid() {
            @Override
            public void visitJetElement(@NotNull JetElement e) {
                e.acceptChildren(this);
            }

            @Override
            public void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expr) {
                DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expr);

                if (isLocalFunction(descriptor)) {
                    lookupInContext(descriptor);
                }
                else if (descriptor instanceof CallableMemberDescriptor) {
                    ResolvedCall<? extends CallableDescriptor> call = getResolvedCall(expr, bindingContext);
                    if (call != null) {
                        lookupReceiver(call.getDispatchReceiver());
                        lookupReceiver(call.getExtensionReceiver());
                    }
                }
                else if (descriptor instanceof VariableDescriptor) {
                    if (descriptor.getContainingDeclaration() instanceof ConstructorDescriptor) {
                        ClassDescriptor classDescriptor =
                                (ClassDescriptor) descriptor.getContainingDeclaration().getContainingDeclaration();
                        if (classDescriptor == ImplementationBodyCodegen.this.descriptor) return;
                    }
                    lookupInContext(descriptor);
                }
            }

            private void lookupReceiver(@NotNull ReceiverValue value) {
                if (value instanceof ThisReceiver) {
                    if (value instanceof ExtensionReceiver) {
                        ReceiverParameterDescriptor parameter =
                                ((ExtensionReceiver) value).getDeclarationDescriptor().getExtensionReceiverParameter();
                        assert parameter != null : "Extension receiver should exist: " + ((ExtensionReceiver) value).getDeclarationDescriptor();
                        lookupInContext(parameter);
                    }
                    else {
                        lookupInContext(((ThisReceiver) value).getDeclarationDescriptor());
                    }
                }
            }


            private void lookupInContext(@NotNull DeclarationDescriptor toLookup) {
                context.lookupInContext(toLookup, StackValue.LOCAL_0, state, true);
            }

            @Override
            public void visitThisExpression(@NotNull JetThisExpression expression) {
                DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.getInstanceReference());
                assert descriptor instanceof CallableDescriptor ||
                       descriptor instanceof ClassDescriptor : "'This' reference target should be class or callable descriptor but was " + descriptor;
                if (descriptor instanceof ClassDescriptor) {
                    lookupInContext(descriptor);
                }

                if (descriptor instanceof CallableDescriptor) {
                    ReceiverParameterDescriptor parameter = ((CallableDescriptor) descriptor).getExtensionReceiverParameter();
                    if (parameter != null) {
                        lookupInContext(parameter);
                    }
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
            else if (declaration instanceof JetSecondaryConstructor) {
                JetSecondaryConstructor constructor = (JetSecondaryConstructor) declaration;
                constructor.accept(visitor);
            }
        }

        for (JetDelegationSpecifier specifier : myClass.getDelegationSpecifiers()) {
            if (specifier instanceof JetDelegatorByExpressionSpecifier) {
                JetExpression delegateExpression = ((JetDelegatorByExpressionSpecifier) specifier).getDelegateExpression();
                assert delegateExpression != null;
                delegateExpression.accept(visitor);
            }
        }

        ClassDescriptor superClass = DescriptorUtilPackage.getSuperClassNotAny(descriptor);
        if (superClass != null) {
            if (superClass.isInner()) {
                context.lookupInContext(superClass.getContainingDeclaration(), StackValue.LOCAL_0, state, true);
            }

            ConstructorDescriptor primaryConstructor = descriptor.getUnsubstitutedPrimaryConstructor();
            if (primaryConstructor != null && !isAnonymousObject(descriptor)) {
                ResolvedCall<ConstructorDescriptor> delegationCall = getDelegationConstructorCall(bindingContext, primaryConstructor);
                JetValueArgumentList argumentList = delegationCall != null ? delegationCall.getCall().getValueArgumentList() : null;
                if (argumentList != null) {
                    argumentList.accept(visitor);
                }
            }
        }
    }

    private void generateTraitMethods() {
        if (isTrait(descriptor)) return;

        for (Map.Entry<FunctionDescriptor, FunctionDescriptor> entry : CodegenUtil.getTraitMethods(descriptor).entrySet()) {
            FunctionDescriptor traitFun = entry.getKey();
            //skip java 8 default methods
            if (!(traitFun instanceof JavaCallableMemberDescriptor)) {
                generateDelegationToTraitImpl(traitFun, entry.getValue());
            }
        }
    }

    private void generateDelegationToTraitImpl(@NotNull final FunctionDescriptor traitFun, @NotNull final FunctionDescriptor inheritedFun) {
        functionCodegen.generateMethod(
                DelegationToTraitImpl(descriptorToDeclaration(traitFun), traitFun),
                inheritedFun,
                new FunctionGenerationStrategy.CodegenBased<FunctionDescriptor>(state, inheritedFun) {
                    @Override
                    public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                        DeclarationDescriptor containingDeclaration = traitFun.getContainingDeclaration();
                        if (!DescriptorUtils.isTrait(containingDeclaration)) return;

                        DeclarationDescriptor declarationInheritedFun = inheritedFun.getContainingDeclaration();
                        PsiElement classForInheritedFun = descriptorToDeclaration(declarationInheritedFun);
                        if (classForInheritedFun instanceof JetDeclaration) {
                            codegen.markLineNumber((JetElement) classForInheritedFun, false);
                        }

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

                        if (KotlinBuiltIns.isCloneable(containingTrait) && traitMethod.getName().equals("clone")) {
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
            @NotNull ConstructorDescriptor constructorDescriptor,
            @Nullable ResolvedCall<ConstructorDescriptor> delegationConstructorCall
    ) {
        if (delegationConstructorCall == null) {
            genSimpleSuperCall(iv);
            return;
        }
        iv.load(0, OBJECT_TYPE);
        ConstructorDescriptor delegateConstructor = SamCodegenUtil.resolveSamAdapter(codegen.getConstructorDescriptor(delegationConstructorCall));

        CallableMethod delegateConstructorCallable = typeMapper.mapToCallableMethod(delegateConstructor);
        CallableMethod callable = typeMapper.mapToCallableMethod(constructorDescriptor);

        List<JvmMethodParameterSignature> delegatingParameters = delegateConstructorCallable.getValueParameters();
        List<JvmMethodParameterSignature> parameters = callable.getValueParameters();

        ArgumentGenerator argumentGenerator;
        if (isSameClassConstructor(delegateConstructor)) {
            // if it's the same class constructor we should just pass all synthetic parameters
            argumentGenerator =
                    generateThisCallImplicitArguments(iv, codegen, delegateConstructor, delegateConstructorCallable, delegatingParameters,
                                                      parameters);
        }
        else {
            argumentGenerator =
                    generateSuperCallImplicitArguments(iv, codegen, constructorDescriptor, delegateConstructor, delegateConstructorCallable,
                                                       delegatingParameters,
                                                       parameters);
        }

        codegen.invokeMethodWithArguments(
                delegateConstructorCallable, delegationConstructorCall, StackValue.none(), codegen.defaultCallGenerator, argumentGenerator);
    }

    private boolean isSameClassConstructor(@Nullable ConstructorDescriptor delegatingConstructor) {
        return delegatingConstructor != null && delegatingConstructor.getContainingDeclaration() == descriptor;
    }

    @NotNull
    private ArgumentGenerator generateSuperCallImplicitArguments(
            @NotNull InstructionAdapter iv,
            @NotNull ExpressionCodegen codegen,
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull ConstructorDescriptor superConstructor,
            @NotNull CallableMethod superCallable,
            @NotNull List<JvmMethodParameterSignature> superParameters,
            @NotNull List<JvmMethodParameterSignature> parameters
    ) {
        int offset = 1;
        int superIndex = 0;

        // Here we match all the super constructor parameters except those with kind VALUE to the derived constructor parameters, push
        // them all onto the stack and update "offset" variable so that in the end it points to the slot of the first VALUE argument
        for (JvmMethodParameterSignature parameter : parameters) {
            if (superIndex >= superParameters.size()) break;

            JvmMethodParameterKind superKind = superParameters.get(superIndex).getKind();
            JvmMethodParameterKind kind = parameter.getKind();
            Type type = parameter.getAsmType();

            if (superKind == JvmMethodParameterKind.VALUE && kind == JvmMethodParameterKind.SUPER_CALL_PARAM) {
                // Stop when we reach the actual value parameters present in the code; they will be generated via ResolvedCall below
                break;
            }

            if (superKind == JvmMethodParameterKind.OUTER) {
                assert kind == JvmMethodParameterKind.OUTER || kind == JvmMethodParameterKind.SUPER_CALL_PARAM :
                        String.format("Non-outer parameter incorrectly mapped to outer for %s: %s vs %s",
                                      constructorDescriptor, parameters, superParameters);
                // Super constructor requires OUTER parameter, but our OUTER instance may be different from what is expected by the super
                // constructor. We need to traverse our outer classes from the bottom up, to find the needed class
                // TODO: isSuper should be "true" but this makes some tests on inner classes extending outer fail
                // See innerExtendsOuter.kt, semantics of inner classes extending their outer should be changed to be as in Java
                ClassDescriptor outerForSuper = (ClassDescriptor) superConstructor.getContainingDeclaration().getContainingDeclaration();
                StackValue outer = codegen.generateThisOrOuter(outerForSuper, false);
                outer.put(outer.type, codegen.v);
                superIndex++;
            }
            else if (kind == JvmMethodParameterKind.SUPER_CALL_PARAM || kind == JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL) {
                iv.load(offset, type);
                superIndex++;
            }

            offset += type.getSize();
        }

        if (isAnonymousObject(descriptor)) {
            List<JvmMethodParameterSignature> superValues = superParameters.subList(superIndex, superParameters.size());
            return new ObjectSuperCallArgumentGenerator(superValues, iv, offset);
        }
        else {
            return new CallBasedArgumentGenerator(codegen, codegen.defaultCallGenerator, superConstructor.getValueParameters(),
                                                   superCallable.getValueParameterTypes());
        }
    }

    @NotNull
    private static ArgumentGenerator generateThisCallImplicitArguments(
            @NotNull InstructionAdapter iv,
            @NotNull ExpressionCodegen codegen,
            @NotNull ConstructorDescriptor delegatingConstructor,
            @NotNull CallableMethod delegatingCallable,
            @NotNull List<JvmMethodParameterSignature> delegatingParameters,
            @NotNull List<JvmMethodParameterSignature> parameters
    ) {
        int offset = 1;
        int index = 0;
        for (; index < delegatingParameters.size(); index++) {
            JvmMethodParameterKind delegatingKind = delegatingParameters.get(index).getKind();
            if (delegatingKind == JvmMethodParameterKind.VALUE) {
                assert index == parameters.size() || parameters.get(index).getKind() == JvmMethodParameterKind.VALUE:
                        "Delegating constructor has not enough implicit parameters";
                break;
            }
            assert index < parameters.size() && parameters.get(index).getKind() == delegatingKind :
                    "Constructors of the same class should have the same set of implicit arguments";
            JvmMethodParameterSignature parameter = parameters.get(index);

            iv.load(offset, parameter.getAsmType());
            offset += parameter.getAsmType().getSize();
        }

        assert index == parameters.size() || parameters.get(index).getKind() == JvmMethodParameterKind.VALUE :
                    "Delegating constructor has not enough parameters";

        return new CallBasedArgumentGenerator(codegen, codegen.defaultCallGenerator, delegatingConstructor.getValueParameters(),
                                              delegatingCallable.getValueParameterTypes());
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

    private void generateEnumEntries() {
        if (descriptor.getKind() != ClassKind.ENUM_CLASS) return;

        List<JetEnumEntry> enumEntries = KotlinPackage.filterIsInstance(element.getDeclarations(), JetEnumEntry.class);

        for (JetEnumEntry enumEntry : enumEntries) {
            ClassDescriptor descriptor = getNotNull(bindingContext, BindingContext.CLASS, enumEntry);
            FieldVisitor fv = v.newField(OtherOrigin(enumEntry, descriptor), ACC_PUBLIC | ACC_ENUM | ACC_STATIC | ACC_FINAL,
                                         descriptor.getName().asString(), classAsmType.getDescriptor(), null, null);
            AnnotationCodegen.forField(fv, typeMapper).genAnnotations(descriptor, null);
        }

        initializeEnumConstants(enumEntries);
    }

    private void initializeEnumConstants(@NotNull List<JetEnumEntry> enumEntries) {
        if (state.getClassBuilderMode() != ClassBuilderMode.FULL) return;

        ExpressionCodegen codegen = createOrGetClInitCodegen();
        InstructionAdapter iv = codegen.v;

        Type arrayAsmType = typeMapper.mapType(getBuiltIns(descriptor).getArrayType(INVARIANT, descriptor.getDefaultType()));
        v.newField(OtherOrigin(myClass), ACC_PRIVATE | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC, ENUM_VALUES_FIELD_NAME,
                   arrayAsmType.getDescriptor(), null, null);

        iv.iconst(enumEntries.size());
        iv.newarray(classAsmType);

        if (!enumEntries.isEmpty()) {
            iv.dup();
            for (int ordinal = 0, size = enumEntries.size(); ordinal < size; ordinal++) {
                initializeEnumConstant(enumEntries, ordinal);
            }
        }

        iv.putstatic(classAsmType.getInternalName(), ENUM_VALUES_FIELD_NAME, arrayAsmType.getDescriptor());
    }

    private void initializeEnumConstant(@NotNull List<JetEnumEntry> enumEntries, int ordinal) {
        ExpressionCodegen codegen = createOrGetClInitCodegen();
        InstructionAdapter iv = codegen.v;
        JetEnumEntry enumEntry = enumEntries.get(ordinal);

        iv.dup();
        iv.iconst(ordinal);

        ClassDescriptor classDescriptor = getNotNull(bindingContext, BindingContext.CLASS, enumEntry);
        Type implClass = typeMapper.mapClass(classDescriptor);

        iv.anew(implClass);
        iv.dup();

        iv.aconst(enumEntry.getName());
        iv.iconst(ordinal);

        List<JetDelegationSpecifier> delegationSpecifiers = enumEntry.getDelegationSpecifiers();
        if (delegationSpecifiers.size() == 1 && !enumEntryNeedSubclass(bindingContext, enumEntry)) {
            ResolvedCall<?> resolvedCall = CallUtilPackage.getResolvedCallWithAssert(delegationSpecifiers.get(0), bindingContext);

            CallableMethod method = typeMapper.mapToCallableMethod((ConstructorDescriptor) resolvedCall.getResultingDescriptor());

            codegen.invokeMethodWithArguments(method, resolvedCall, StackValue.none());
        }
        else {
            iv.invokespecial(implClass.getInternalName(), "<init>", "(Ljava/lang/String;I)V", false);
        }

        iv.dup();
        iv.putstatic(classAsmType.getInternalName(), enumEntry.getName(), classAsmType.getDescriptor());
        iv.astore(OBJECT_TYPE);
    }

    private void generateDelegates(DelegationFieldsInfo delegationFieldsInfo) {
        for (JetDelegationSpecifier specifier : myClass.getDelegationSpecifiers()) {
            if (specifier instanceof JetDelegatorByExpressionSpecifier) {
                DelegationFieldsInfo.Field field = delegationFieldsInfo.getInfo((JetDelegatorByExpressionSpecifier) specifier);
                generateDelegateField(field);
                JetExpression delegateExpression = ((JetDelegatorByExpressionSpecifier) specifier).getDelegateExpression();
                JetType delegateExpressionType = delegateExpression != null ? bindingContext.getType(delegateExpression) : null;
                generateDelegates(getSuperClass(specifier), delegateExpressionType, field);
            }
        }
    }

    private void generateDelegateField(DelegationFieldsInfo.Field fieldInfo) {
        if (!fieldInfo.generateField) return;

        v.newField(JvmDeclarationOrigin.NO_ORIGIN, ACC_PRIVATE | ACC_FINAL | ACC_SYNTHETIC,
                   fieldInfo.name, fieldInfo.type.getDescriptor(), /*TODO*/null, null);
    }

    protected void generateDelegates(ClassDescriptor toTrait, JetType delegateExpressionType, DelegationFieldsInfo.Field field) {
        for (Map.Entry<CallableMemberDescriptor, CallableDescriptor> entry : CodegenUtilKt.getDelegates(descriptor, toTrait, delegateExpressionType).entrySet()) {
            CallableMemberDescriptor callableMemberDescriptor = entry.getKey();
            CallableDescriptor delegateTo = entry.getValue();
            if (callableMemberDescriptor instanceof PropertyDescriptor) {
                propertyCodegen
                        .genDelegate((PropertyDescriptor) callableMemberDescriptor, (PropertyDescriptor) delegateTo, field.getStackValue());
            }
            else if (callableMemberDescriptor instanceof FunctionDescriptor) {
                functionCodegen
                        .genDelegate((FunctionDescriptor) callableMemberDescriptor, (FunctionDescriptor) delegateTo, field.getStackValue());
            }
        }
    }

    public void addCompanionObjectPropertyToCopy(@NotNull PropertyDescriptor descriptor, Object defaultValue) {
        if (companionObjectPropertiesToCopy == null) {
            companionObjectPropertiesToCopy = new ArrayList<PropertyAndDefaultValue>();
        }
        companionObjectPropertiesToCopy.add(new PropertyAndDefaultValue(descriptor, defaultValue));
    }

    @Override
    protected void done() {
        for (Function2<ImplementationBodyCodegen, ClassBuilder, Unit> task : additionalTasks) {
            task.invoke(this, v);
        }

        super.done();
    }

    private static class PropertyAndDefaultValue {
        public final PropertyDescriptor descriptor;
        public final Object defaultValue;

        public PropertyAndDefaultValue(@NotNull PropertyDescriptor descriptor, Object defaultValue) {
            this.descriptor = descriptor;
            this.defaultValue = defaultValue;
        }
    }

    public void addAdditionalTask(Function2<ImplementationBodyCodegen, ClassBuilder, Unit> additionalTask) {
        additionalTasks.add(additionalTask);
    }
}
