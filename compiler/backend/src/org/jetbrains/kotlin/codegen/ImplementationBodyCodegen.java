/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.backend.common.DataClassMethodGenerator;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.codegen.binding.MutableClosure;
import org.jetbrains.kotlin.codegen.context.*;
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension;
import org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil;
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension;
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter;
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor;
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor;
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmClassSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.serialization.DescriptorSerializer;
import org.jetbrains.kotlin.serialization.ProtoBuf;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.*;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.FQ_NAMES;
import static org.jetbrains.kotlin.codegen.AsmUtil.*;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.*;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.enumEntryNeedSubclass;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.getDelegationConstructorCall;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.getNotNull;
import static org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.descriptorToDeclaration;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.NO_ORIGIN;
import static org.jetbrains.kotlin.types.Variance.INVARIANT;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.isLocalFunction;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class ImplementationBodyCodegen extends ClassBodyCodegen {
    private static final String ENUM_VALUES_FIELD_NAME = "$VALUES";
    private Type superClassAsmType;
    @Nullable // null means java/lang/Object
    private KotlinType superClassType;
    private final Type classAsmType;
    private final boolean isLocal;

    private List<PropertyAndDefaultValue> companionObjectPropertiesToCopy;

    private final DelegationFieldsInfo delegationFieldsInfo;

    private final List<Function2<ImplementationBodyCodegen, ClassBuilder, Unit>> additionalTasks =
            new ArrayList<Function2<ImplementationBodyCodegen, ClassBuilder, Unit>>();

    public ImplementationBodyCodegen(
            @NotNull KtClassOrObject aClass,
            @NotNull ClassContext context,
            @NotNull ClassBuilder v,
            @NotNull GenerationState state,
            @Nullable MemberCodegen<?> parentCodegen,
            boolean isLocal
    ) {
        super(aClass, context, v, state, parentCodegen);
        this.classAsmType = typeMapper.mapClass(descriptor);
        this.isLocal = isLocal;
        delegationFieldsInfo = getDelegationFieldsInfo(myClass.getSuperTypeListEntries());
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

        if (myClass instanceof KtClass) {
            KtClass ktClass = (KtClass) myClass;
            if (ktClass.hasModifier(KtTokens.ABSTRACT_KEYWORD) || ktClass.isSealed()) {
                isAbstract = true;
            }
            if (ktClass.isInterface()) {
                isAbstract = true;
                isInterface = true;
            }
            else if (descriptor.getKind() == ClassKind.ANNOTATION_CLASS) {
                isAbstract = true;
                isInterface = true;
                isAnnotation = true;
            }
            else if (ktClass.isEnum()) {
                isAbstract = hasAbstractMembers(descriptor);
                isEnum = true;
            }

            if (isObject(descriptor)) {
                isFinal = true;
            }

            if (!ktClass.hasModifier(KtTokens.OPEN_KEYWORD) && !isAbstract) {
                // Light-class mode: Do not make enum classes final since PsiClass corresponding to enum is expected to be inheritable from
                isFinal = !(ktClass.isEnum() && !state.getClassBuilderMode().generateBodies);
            }
            isStatic = !ktClass.isInner();
        }
        else {
            isStatic = isCompanionObject(descriptor);
            isFinal = true;
        }

        int access = 0;

        if (!state.getClassBuilderMode().generateBodies && !DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            // !ClassBuilderMode.generateBodies means we are generating light classes & looking at a nested or inner class
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
            for (KtDeclaration declaration : myClass.getDeclarations()) {
                if (declaration instanceof KtEnumEntry) {
                    if (enumEntryNeedSubclass(bindingContext, (KtEnumEntry) declaration)) {
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

        generateEnumEntries();
    }

    @Override
    protected void generateDefaultImplsIfNeeded() {
        if (isInterface(descriptor) && !isLocal) {
            Type defaultImplsType = state.getTypeMapper().mapDefaultImpls(descriptor);
            ClassBuilder defaultImplsBuilder =
                    state.getFactory().newVisitor(JvmDeclarationOriginKt.TraitImpl(myClass, descriptor), defaultImplsType, myClass.getContainingFile());

            CodegenContext parentContext = context.getParentContext();
            assert parentContext != null : "Parent context of interface declaration should not be null";

            ClassContext defaultImplsContext = parentContext.intoDefaultImplsClass(descriptor, (ClassContext) context, state);
            new InterfaceImplBodyCodegen(myClass, defaultImplsContext, defaultImplsBuilder, state, this).generate();
        }
    }

    @Override
    protected void generateKotlinMetadataAnnotation() {
        final DescriptorSerializer serializer =
                DescriptorSerializer.create(descriptor, new JvmSerializerExtension(v.getSerializationBindings(), state));

        final ProtoBuf.Class classProto = serializer.classProto(descriptor).build();

        WriteAnnotationUtilKt.writeKotlinMetadata(v, KotlinClassHeader.Kind.CLASS, new Function1<AnnotationVisitor, Unit>() {
            @Override
            public Unit invoke(AnnotationVisitor av) {
                writeAnnotationData(av, serializer, classProto);
                return Unit.INSTANCE;
            }
        });
    }

    private void writeEnclosingMethod() {
        // Do not emit enclosing method in "light-classes mode" since currently we generate local light classes as if they're top level
        if (!state.getClassBuilderMode().generateBodies) {
            return;
        }

        //JVMS7: A class must have an EnclosingMethod attribute if and only if it is a local class or an anonymous class.
        if (isAnonymousObject(descriptor) || !(descriptor.getContainingDeclaration() instanceof ClassOrPackageFragmentDescriptor)) {
            writeOuterClassAndEnclosingMethod();
        }
    }

    private static final Map<FqName, String> KOTLIN_MARKER_INTERFACES = new HashMap<FqName, String>();
    static {
        KOTLIN_MARKER_INTERFACES.put(FQ_NAMES.iterator, "kotlin/jvm/internal/markers/KMappedMarker");
        KOTLIN_MARKER_INTERFACES.put(FQ_NAMES.iterable, "kotlin/jvm/internal/markers/KMappedMarker");
        KOTLIN_MARKER_INTERFACES.put(FQ_NAMES.collection, "kotlin/jvm/internal/markers/KMappedMarker");
        KOTLIN_MARKER_INTERFACES.put(FQ_NAMES.list, "kotlin/jvm/internal/markers/KMappedMarker");
        KOTLIN_MARKER_INTERFACES.put(FQ_NAMES.listIterator, "kotlin/jvm/internal/markers/KMappedMarker");
        KOTLIN_MARKER_INTERFACES.put(FQ_NAMES.set, "kotlin/jvm/internal/markers/KMappedMarker");
        KOTLIN_MARKER_INTERFACES.put(FQ_NAMES.map, "kotlin/jvm/internal/markers/KMappedMarker");
        KOTLIN_MARKER_INTERFACES.put(FQ_NAMES.mapEntry, "kotlin/jvm/internal/markers/KMappedMarker");

        KOTLIN_MARKER_INTERFACES.put(FQ_NAMES.mutableIterator, "kotlin/jvm/internal/markers/KMutableIterator");
        KOTLIN_MARKER_INTERFACES.put(FQ_NAMES.mutableIterable, "kotlin/jvm/internal/markers/KMutableIterable");
        KOTLIN_MARKER_INTERFACES.put(FQ_NAMES.mutableCollection, "kotlin/jvm/internal/markers/KMutableCollection");
        KOTLIN_MARKER_INTERFACES.put(FQ_NAMES.mutableList, "kotlin/jvm/internal/markers/KMutableList");
        KOTLIN_MARKER_INTERFACES.put(FQ_NAMES.mutableListIterator, "kotlin/jvm/internal/markers/KMutableListIterator");
        KOTLIN_MARKER_INTERFACES.put(FQ_NAMES.mutableSet, "kotlin/jvm/internal/markers/KMutableSet");
        KOTLIN_MARKER_INTERFACES.put(FQ_NAMES.mutableMap, "kotlin/jvm/internal/markers/KMutableMap");
        KOTLIN_MARKER_INTERFACES.put(FQ_NAMES.mutableMapEntry, "kotlin/jvm/internal/markers/KMutableMap$Entry");
    }

    @NotNull
    private JvmClassSignature signature() {
        JvmSignatureWriter sw = new BothSignatureWriter(BothSignatureWriter.Mode.CLASS);

        typeMapper.writeFormalTypeParameters(descriptor.getDeclaredTypeParameters(), sw);

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
        Set<String> kotlinMarkerInterfaces = new LinkedHashSet<String>();

        for (KotlinType supertype : descriptor.getTypeConstructor().getSupertypes()) {
            if (isJvmInterface(supertype.getConstructor().getDeclarationDescriptor())) {
                sw.writeInterface();
                Type jvmInterfaceType = typeMapper.mapSupertype(supertype, sw);
                sw.writeInterfaceEnd();
                String jvmInterfaceInternalName = jvmInterfaceType.getInternalName();
                superInterfaces.add(jvmInterfaceInternalName);

                FqName kotlinInterfaceName = DescriptorUtils.getFqName(supertype.getConstructor().getDeclarationDescriptor()).toSafe();
                String kotlinMarkerInterfaceInternalName = KOTLIN_MARKER_INTERFACES.get(kotlinInterfaceName);
                if (kotlinMarkerInterfaceInternalName != null) {
                    kotlinMarkerInterfaces.add(kotlinMarkerInterfaceInternalName);
                }
            }
        }

        for (String kotlinMarkerInterface : kotlinMarkerInterfaces) {
            sw.writeInterface();
            sw.writeAsmType(Type.getObjectType(kotlinMarkerInterface));
            sw.writeInterfaceEnd();
        }

        superInterfaces.addAll(kotlinMarkerInterfaces);

        return new JvmClassSignature(classAsmType.getInternalName(), superClassAsmType.getInternalName(),
                                     new ArrayList<String>(superInterfaces), sw.makeJavaGenericSignature());
    }

    protected void getSuperClass() {
        superClassAsmType = OBJECT_TYPE;
        superClassType = null;

        if (descriptor.getKind() == ClassKind.INTERFACE) {
            return;
        }

        for (KotlinType supertype : descriptor.getTypeConstructor().getSupertypes()) {
            ClassifierDescriptor superClass = supertype.getConstructor().getDeclarationDescriptor();
            if (superClass != null && !isJvmInterface(superClass)) {
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

        generateTraitMethods();

        generateDelegates(delegationFieldsInfo);

        if (!isInterface(descriptor)  || kind == OwnerKind.DEFAULT_IMPLS) {
            generateSyntheticAccessors();
        }

        generateEnumMethods();

        generateFunctionsForDataClasses();

        new CollectionStubMethodGenerator(typeMapper, descriptor).generate(functionCodegen, v);

        generateToArray();

        genClosureFields(context.closure, v, typeMapper);

        for (ExpressionCodegenExtension extension : ExpressionCodegenExtension.Companion.getInstances(state.getProject())) {
            extension.generateClassSyntheticParts(v, state, myClass, descriptor);
        }
    }

    @Override
    protected void generateConstructors() {
        try {
            lookupConstructorExpressionsInClosureIfPresent();
            generatePrimaryConstructor(delegationFieldsInfo);
            for (ConstructorDescriptor secondaryConstructor : DescriptorUtilsKt.getSecondaryConstructors(descriptor)) {
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
    }

    private boolean isGenericToArray(@NotNull FunctionDescriptor function) {
        if (function.getValueParameters().size() != 1 || function.getTypeParameters().size() != 1) {
            return false;
        }

        KotlinType returnType = function.getReturnType();
        assert returnType != null : function.toString();
        KotlinType paramType = function.getValueParameters().get(0).getType();
        if (KotlinBuiltIns.isArray(returnType) && KotlinBuiltIns.isArray(paramType)) {
            KotlinType elementType = function.getTypeParameters().get(0).getDefaultType();
            KotlinBuiltIns builtIns = DescriptorUtilsKt.getBuiltIns(descriptor);
            if (KotlinTypeChecker.DEFAULT.equalTypes(elementType, builtIns.getArrayElementType(returnType))
                && KotlinTypeChecker.DEFAULT.equalTypes(elementType, builtIns.getArrayElementType(paramType))) {
                return true;
            }
        }

        return false;
    }

    private static boolean isNonGenericToArray(@NotNull FunctionDescriptor function) {
        if (!function.getValueParameters().isEmpty() || !function.getTypeParameters().isEmpty()) {
            return false;
        }

        KotlinType returnType = function.getReturnType();
        return returnType != null && KotlinBuiltIns.isArray(returnType);
    }

    private void generateToArray() {
        if (descriptor.getKind() == ClassKind.INTERFACE) return;

        final KotlinBuiltIns builtIns = DescriptorUtilsKt.getBuiltIns(descriptor);
        if (!isSubclass(descriptor, builtIns.getCollection())) return;

        if (CollectionsKt.any(DescriptorUtilsKt.getAllSuperclassesWithoutAny(descriptor), new Function1<ClassDescriptor, Boolean>() {
            @Override
            public Boolean invoke(ClassDescriptor classDescriptor) {
                return !(classDescriptor instanceof JavaClassDescriptor) && isSubclass(classDescriptor, builtIns.getCollection());
            }
        })) return;

        Collection<SimpleFunctionDescriptor> functions = descriptor.getDefaultType().getMemberScope().getContributedFunctions(
                Name.identifier("toArray"), NoLookupLocation.FROM_BACKEND
        );
        boolean hasGenericToArray = false;
        boolean hasNonGenericToArray = false;
        for (FunctionDescriptor function : functions) {
            hasGenericToArray |= isGenericToArray(function);
            hasNonGenericToArray |= isNonGenericToArray(function);
        }

        if (!hasNonGenericToArray) {
            MethodVisitor mv = v.newMethod(NO_ORIGIN, ACC_PUBLIC, "toArray", "()[Ljava/lang/Object;", null, null);

            InstructionAdapter iv = new InstructionAdapter(mv);
            mv.visitCode();

            iv.load(0, classAsmType);
            iv.invokestatic("kotlin/jvm/internal/CollectionToArray", "toArray", "(Ljava/util/Collection;)[Ljava/lang/Object;", false);
            iv.areturn(Type.getType("[Ljava/lang/Object;"));

            FunctionCodegen.endVisit(mv, "toArray", myClass);
        }

        if (!hasGenericToArray) {
            MethodVisitor mv = v.newMethod(
                    NO_ORIGIN, ACC_PUBLIC, "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", "<T:Ljava/lang/Object;>([TT;)[TT;", null);

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

    private void generateFunctionsForDataClasses() {
        if (!descriptor.isData()) return;

        new DataClassMethodGeneratorImpl(myClass, bindingContext).generate();
    }

    private class DataClassMethodGeneratorImpl extends DataClassMethodGenerator {
        DataClassMethodGeneratorImpl(
                KtClassOrObject klass,
                BindingContext bindingContext
        ) {
            super(klass, bindingContext);
        }

        @Override
        public void generateEqualsMethod(@NotNull FunctionDescriptor function, @NotNull List<? extends PropertyDescriptor> properties) {
            MethodContext context = ImplementationBodyCodegen.this.context.intoFunction(function);
            MethodVisitor mv = v.newMethod(JvmDeclarationOriginKt.OtherOrigin(function), ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
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

                if (asmType.getSort() == Type.FLOAT) {
                    iv.invokestatic("java/lang/Float", "compare", "(FF)I", false);
                    iv.ifne(ne);
                }
                else if (asmType.getSort() == Type.DOUBLE) {
                    iv.invokestatic("java/lang/Double", "compare", "(DD)I", false);
                    iv.ifne(ne);
                }
                else {
                    StackValue value = genEqualsForExpressionsOnStack(KtTokens.EQEQ, StackValue.onStack(asmType), StackValue.onStack(asmType));
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
        public void generateHashCodeMethod(@NotNull FunctionDescriptor function, @NotNull List<? extends PropertyDescriptor> properties) {
            MethodContext context = ImplementationBodyCodegen.this.context.intoFunction(function);
            MethodVisitor mv = v.newMethod(JvmDeclarationOriginKt.OtherOrigin(function), ACC_PUBLIC, "hashCode", "()I", null, null);
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
        public void generateToStringMethod(@NotNull FunctionDescriptor function, @NotNull List<? extends PropertyDescriptor> properties) {
            MethodContext context = ImplementationBodyCodegen.this.context.intoFunction(function);
            MethodVisitor mv = v.newMethod(JvmDeclarationOriginKt.OtherOrigin(function), ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
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
                return type;
            }
            else {
                //noinspection ConstantConditions
                Method method = typeMapper.mapAsmMethod(propertyDescriptor.getGetter());
                iv.invokevirtual(classAsmType.getInternalName(), method.getName(), method.getDescriptor(), false);
                return method.getReturnType();
            }
        }

        @Override
        public void generateComponentFunction(@NotNull FunctionDescriptor function, @NotNull final ValueParameterDescriptor parameter) {
            PsiElement originalElement = DescriptorToSourceUtils.descriptorToDeclaration(parameter);
            functionCodegen.generateMethod(JvmDeclarationOriginKt.OtherOrigin(originalElement, function), function, new FunctionGenerationStrategy() {
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
        public void generateCopyFunction(
                @NotNull final FunctionDescriptor function,
                @NotNull List<? extends KtParameter> constructorParameters
        ) {
            final Type thisDescriptorType = typeMapper.mapType(descriptor);

            functionCodegen.generateMethod(JvmDeclarationOriginKt.OtherOrigin(myClass, function), function, new FunctionGenerationStrategy() {
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

                    Method constructorAsmMethod = typeMapper.mapAsmMethod(constructor);
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

                    KotlinType captureReceiver = closure.getCaptureReceiverType();
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
                            assert ((ClassDescriptor) function.getContainingDeclaration()).isData()
                                    : "Function container must have [data] modifier: " + function;
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
        Type type = typeMapper.mapType(DescriptorUtilsKt.getBuiltIns(descriptor).getArrayType(INVARIANT, descriptor.getDefaultType()));

        FunctionDescriptor valuesFunction =
                CollectionsKt.single(descriptor.getStaticScope().getContributedFunctions(ENUM_VALUES, NoLookupLocation.FROM_BACKEND));
        MethodVisitor mv = v.newMethod(
                JvmDeclarationOriginKt.OtherOrigin(myClass, valuesFunction), ACC_PUBLIC | ACC_STATIC, ENUM_VALUES.asString(),
                "()" + type.getDescriptor(), null, null
        );
        if (!state.getClassBuilderMode().generateBodies) return;

        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, classAsmType.getInternalName(), ENUM_VALUES_FIELD_NAME, type.getDescriptor());
        mv.visitMethodInsn(INVOKEVIRTUAL, type.getInternalName(), "clone", "()Ljava/lang/Object;", false);
        mv.visitTypeInsn(CHECKCAST, type.getInternalName());
        mv.visitInsn(ARETURN);
        FunctionCodegen.endVisit(mv, "values()", myClass);
    }

    private void generateEnumValueOfMethod() {
        FunctionDescriptor valueOfFunction =
                CollectionsKt.single(descriptor.getStaticScope().getContributedFunctions(ENUM_VALUE_OF, NoLookupLocation.FROM_BACKEND), new Function1<FunctionDescriptor, Boolean>() {
                    @Override
                    public Boolean invoke(FunctionDescriptor descriptor) {
                        return CodegenUtil.isEnumValueOfMethod(descriptor);
                    }
                });
        MethodVisitor mv = v.newMethod(JvmDeclarationOriginKt.OtherOrigin(myClass, valueOfFunction), ACC_PUBLIC | ACC_STATIC, ENUM_VALUE_OF.asString(),
                                       "(Ljava/lang/String;)" + classAsmType.getDescriptor(), null, null);
        if (!state.getClassBuilderMode().generateBodies) return;

        mv.visitCode();
        mv.visitLdcInsn(classAsmType);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;", false);
        mv.visitTypeInsn(CHECKCAST, classAsmType.getInternalName());
        mv.visitInsn(ARETURN);
        FunctionCodegen.endVisit(mv, "valueOf()", myClass);
    }

    private void generateFieldForSingleton() {
        if (isEnumEntry(descriptor) || isCompanionObject(descriptor)) return;

        if (isNonCompanionObject(descriptor)) {
            StackValue.Field field = StackValue.singletonViaInstance(descriptor, typeMapper);
            v.newField(JvmDeclarationOriginKt.OtherOrigin(myClass),
                       ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
                       field.name, field.type.getDescriptor(), null, null);

            if (!state.getClassBuilderMode().generateBodies) return;
            // Invoke the object constructor but ignore the result because INSTANCE will be initialized in the first line of <init>
            InstructionAdapter v = createOrGetClInitCodegen().v;
            markLineNumberForElement(element, v);
            v.anew(classAsmType);
            v.invokespecial(classAsmType.getInternalName(), "<init>", "()V", false);

            return;
        }

        ClassDescriptor companionObjectDescriptor = descriptor.getCompanionObjectDescriptor();
        if (companionObjectDescriptor == null) {
            return;
        }

        KtObjectDeclaration companionObject = CollectionsKt.firstOrNull(((KtClass) myClass).getCompanionObjects());
        assert companionObject != null : "Companion object not found: " + myClass.getText();

        StackValue.Field field = StackValue.singleton(companionObjectDescriptor, typeMapper);
        v.newField(JvmDeclarationOriginKt.OtherOrigin(companionObject), ACC_PUBLIC | ACC_STATIC | ACC_FINAL, field.name, field.type.getDescriptor(), null, null);
    }

    private void generateCompanionObjectBackingFieldCopies() {
        if (companionObjectPropertiesToCopy == null) return;

        for (PropertyAndDefaultValue info : companionObjectPropertiesToCopy) {
            PropertyDescriptor property = info.descriptor;

            Type type = typeMapper.mapType(property);
            int modifiers = ACC_STATIC | ACC_FINAL | ACC_PUBLIC;
            FieldVisitor fv = v.newField(JvmDeclarationOriginKt.Synthetic(DescriptorToSourceUtils.descriptorToDeclaration(property), property),
                                         modifiers, context.getFieldName(property, false),
                                         type.getDescriptor(), typeMapper.mapFieldSignature(property.getType(), property),
                                         info.defaultValue);

            AnnotationCodegen.forField(fv, typeMapper).genAnnotations(property, type);

            //This field are always static and final so if it has constant initializer don't do anything in clinit,
            //field would be initialized via default value in v.newField(...) - see JVM SPEC Ch.4
            // TODO: test this code
            if (state.getClassBuilderMode().generateBodies && info.defaultValue == null) {
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

        FunctionDescriptor constructor = (FunctionDescriptor) context.accessibleDescriptor(
                CollectionsKt.single(companionObject.getConstructors()), /* superCallExpression = */ null
        );
        generateMethodCallTo(constructor, null, codegen.v);
        StackValue instance = StackValue.onStack(typeMapper.mapClass(companionObject));
        StackValue.singleton(companionObject, typeMapper).store(instance, codegen.v, true);
    }

    private void generatePrimaryConstructor(final DelegationFieldsInfo delegationFieldsInfo) {
        if (isInterface(descriptor) || isAnnotationClass(descriptor)) return;

        ConstructorDescriptor constructorDescriptor = descriptor.getUnsubstitutedPrimaryConstructor();
        if (constructorDescriptor == null) return;

        ConstructorContext constructorContext = context.intoConstructor(constructorDescriptor);

        final KtPrimaryConstructor primaryConstructor = myClass.getPrimaryConstructor();
        JvmDeclarationOrigin origin = JvmDeclarationOriginKt
                .OtherOrigin(primaryConstructor != null ? primaryConstructor : myClass, constructorDescriptor);
        functionCodegen.generateMethod(origin, constructorDescriptor, constructorContext,
                   new FunctionGenerationStrategy.CodegenBased<ConstructorDescriptor>(state, constructorDescriptor) {
                       @Override
                       public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                           generatePrimaryConstructorImpl(callableDescriptor, codegen, delegationFieldsInfo, primaryConstructor);
                       }
                   }
        );

        functionCodegen.generateDefaultIfNeeded(constructorContext, constructorDescriptor, OwnerKind.IMPLEMENTATION,
                                                DefaultParameterValueLoader.DEFAULT, null);

        new DefaultParameterValueSubstitutor(state).generatePrimaryConstructorOverloadsIfNeeded(constructorDescriptor, v, kind, myClass);
    }

    private void generateSecondaryConstructor(@NotNull ConstructorDescriptor constructorDescriptor) {
        if (!canHaveDeclaredConstructors(descriptor)) return;

        ConstructorContext constructorContext = context.intoConstructor(constructorDescriptor);

        KtSecondaryConstructor constructor = (KtSecondaryConstructor) descriptorToDeclaration(constructorDescriptor);

        functionCodegen.generateMethod(
                JvmDeclarationOriginKt.OtherOrigin(constructor, constructorDescriptor),
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

        new DefaultParameterValueSubstitutor(state).generateOverloadsIfNeeded(
                constructor, constructorDescriptor, constructorDescriptor, kind, v
        );
    }

    private void generatePrimaryConstructorImpl(
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull ExpressionCodegen codegen,
            @NotNull DelegationFieldsInfo fieldsInfo,
            @Nullable KtPrimaryConstructor primaryConstructor
    ) {
        InstructionAdapter iv = codegen.v;

        markLineNumberForConstructor(constructorDescriptor, primaryConstructor, codegen);

        generateClosureInitialization(iv);

        generateDelegatorToConstructorCall(iv, codegen, constructorDescriptor,
                                           getDelegationConstructorCall(bindingContext, constructorDescriptor));

        if (isNonCompanionObject(descriptor)) {
            StackValue.singletonViaInstance(descriptor, typeMapper).store(StackValue.LOCAL_0, iv);
        }

        for (KtSuperTypeListEntry specifier : myClass.getSuperTypeListEntries()) {
            if (specifier instanceof KtDelegatedSuperTypeEntry) {
                genCallToDelegatorByExpressionSpecifier(iv, codegen, (KtDelegatedSuperTypeEntry) specifier, fieldsInfo);
            }
        }

        int curParam = 0;
        List<ValueParameterDescriptor> parameters = constructorDescriptor.getValueParameters();
        for (KtParameter parameter : getPrimaryConstructorParameters()) {
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

        if (isCompanionObject(descriptor)) {
            ImplementationBodyCodegen parentCodegen = (ImplementationBodyCodegen) getParentCodegen();
            parentCodegen.generateCompanionObjectInitializer(descriptor);
        }

        if (JvmAbi.isCompanionObjectWithBackingFieldsInOuter(descriptor)) {
            final ImplementationBodyCodegen parentCodegen = (ImplementationBodyCodegen) getParentCodegen();
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

        KtSecondaryConstructor constructor =
                (KtSecondaryConstructor) DescriptorToSourceUtils.descriptorToDeclaration(constructorDescriptor);

        markLineNumberForConstructor(constructorDescriptor, constructor, codegen);

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

        assert constructor != null;
        if (constructor.hasBody()) {
            codegen.gen(constructor.getBodyExpression(), Type.VOID_TYPE);
        }

        iv.visitInsn(RETURN);
    }

    private static void markLineNumberForConstructor(
            @NotNull ConstructorDescriptor descriptor,
            @Nullable KtConstructor constructor,
            @NotNull ExpressionCodegen codegen
    ) {
        if (constructor == null) {
            markLineNumberForDescriptor(descriptor.getContainingDeclaration(), codegen.v);
        }
        else if (constructor.hasBody() && !(constructor instanceof KtSecondaryConstructor && !((KtSecondaryConstructor) constructor).hasImplicitDelegationCall())) {
            KtBlockExpression bodyExpression = constructor.getBodyExpression();
            List<KtExpression> statements = bodyExpression != null ? bodyExpression.getStatements() : Collections.<KtExpression>emptyList();
            if (!statements.isEmpty()) {
                codegen.markStartLineNumber(statements.iterator().next());
            }
            else {
                codegen.markStartLineNumber(bodyExpression != null ? bodyExpression : constructor);
            }
        }
        else {
            codegen.markStartLineNumber(constructor);
        }
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
        private final Map<KtDelegatedSuperTypeEntry, Field> fields = new HashMap<KtDelegatedSuperTypeEntry, Field>();

        @NotNull
        public Field getInfo(KtDelegatedSuperTypeEntry specifier) {
            return fields.get(specifier);
        }

        private void addField(KtDelegatedSuperTypeEntry specifier, PropertyDescriptor propertyDescriptor) {
            fields.put(specifier,
                       new Field(typeMapper.mapType(propertyDescriptor), propertyDescriptor.getName().asString(), false));
        }

        private void addField(KtDelegatedSuperTypeEntry specifier, Type type, String name) {
            fields.put(specifier, new Field(type, name, true));
        }
    }

    @NotNull
    private DelegationFieldsInfo getDelegationFieldsInfo(@NotNull List<KtSuperTypeListEntry> delegationSpecifiers) {
        DelegationFieldsInfo result = new DelegationFieldsInfo();
        int n = 0;
        for (KtSuperTypeListEntry specifier : delegationSpecifiers) {
            if (specifier instanceof KtDelegatedSuperTypeEntry) {
                KtExpression expression = ((KtDelegatedSuperTypeEntry) specifier).getDelegateExpression();
                PropertyDescriptor propertyDescriptor = CodegenUtil.getDelegatePropertyIfAny(expression, descriptor, bindingContext);


                if (CodegenUtil.isFinalPropertyWithBackingField(propertyDescriptor, bindingContext)) {
                    result.addField((KtDelegatedSuperTypeEntry) specifier, propertyDescriptor);
                }
                else {
                    KotlinType expressionType = expression != null ? bindingContext.getType(expression) : null;
                    Type asmType =
                            expressionType != null ? typeMapper.mapType(expressionType) : typeMapper.mapType(getSuperClass(specifier));
                    result.addField((KtDelegatedSuperTypeEntry) specifier, asmType, JvmAbi.DELEGATE_SUPER_FIELD_PREFIX + n);
                }
                n++;
            }
        }
        return result;
    }

    @NotNull
    private ClassDescriptor getSuperClass(@NotNull KtSuperTypeListEntry specifier) {
        return CodegenUtil.getSuperClassBySuperTypeListEntry(specifier, bindingContext);
    }

    private void genCallToDelegatorByExpressionSpecifier(
            InstructionAdapter iv,
            ExpressionCodegen codegen,
            KtDelegatedSuperTypeEntry specifier,
            DelegationFieldsInfo fieldsInfo
    ) {
        KtExpression expression = specifier.getDelegateExpression();

        DelegationFieldsInfo.Field fieldInfo = fieldsInfo.getInfo(specifier);
        if (fieldInfo.generateField) {
            iv.load(0, classAsmType);
            fieldInfo.getStackValue().store(codegen.gen(expression), iv);
        }
    }

    private void lookupConstructorExpressionsInClosureIfPresent() {
        if (!state.getClassBuilderMode().generateBodies || descriptor.getConstructors().isEmpty()) return;

        KtVisitorVoid visitor = new KtVisitorVoid() {
            @Override
            public void visitKtElement(@NotNull KtElement e) {
                e.acceptChildren(this);
            }

            @Override
            public void visitSimpleNameExpression(@NotNull KtSimpleNameExpression expr) {
                DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expr);

                if (isLocalFunction(descriptor)) {
                    lookupInContext(descriptor);
                }
                else if (descriptor instanceof CallableMemberDescriptor) {
                    ResolvedCall<? extends CallableDescriptor> call = CallUtilKt.getResolvedCall(expr, bindingContext);
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

            private void lookupReceiver(@Nullable ReceiverValue value) {
                if (value instanceof ImplicitReceiver) {
                    if (value instanceof ExtensionReceiver) {
                        ReceiverParameterDescriptor parameter =
                                ((ExtensionReceiver) value).getDeclarationDescriptor().getExtensionReceiverParameter();
                        assert parameter != null : "Extension receiver should exist: " + ((ExtensionReceiver) value).getDeclarationDescriptor();
                        lookupInContext(parameter);
                    }
                    else {
                        lookupInContext(((ImplicitReceiver) value).getDeclarationDescriptor());
                    }
                }
            }


            private void lookupInContext(@NotNull DeclarationDescriptor toLookup) {
                context.lookupInContext(toLookup, StackValue.LOCAL_0, state, true);
            }

            @Override
            public void visitThisExpression(@NotNull KtThisExpression expression) {
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

            @Override
            public void visitSuperExpression(@NotNull KtSuperExpression expression) {
                lookupInContext(ExpressionCodegen.getSuperCallLabelTarget(context, expression));
            }
        };

        for (KtDeclaration declaration : myClass.getDeclarations()) {
            if (declaration instanceof KtProperty) {
                KtProperty property = (KtProperty) declaration;
                KtExpression initializer = property.getDelegateExpressionOrInitializer();
                if (initializer != null) {
                    initializer.accept(visitor);
                }
            }
            else if (declaration instanceof KtAnonymousInitializer) {
                KtAnonymousInitializer initializer = (KtAnonymousInitializer) declaration;
                initializer.accept(visitor);
            }
            else if (declaration instanceof KtSecondaryConstructor) {
                KtSecondaryConstructor constructor = (KtSecondaryConstructor) declaration;
                constructor.accept(visitor);
            }
        }

        for (KtSuperTypeListEntry specifier : myClass.getSuperTypeListEntries()) {
            if (specifier instanceof KtDelegatedSuperTypeEntry) {
                KtExpression delegateExpression = ((KtDelegatedSuperTypeEntry) specifier).getDelegateExpression();
                assert delegateExpression != null;
                delegateExpression.accept(visitor);
            }
            else if (specifier instanceof KtSuperTypeCallEntry) {
                specifier.accept(visitor);
            }
        }
    }

    private void generateTraitMethods() {
        if (isInterface(descriptor)) return;

        for (Map.Entry<FunctionDescriptor, FunctionDescriptor> entry : CodegenUtil.getNonPrivateTraitMethods(descriptor).entrySet()) {
            FunctionDescriptor traitFun = entry.getKey();
            //skip java 8 default methods
            if (!(traitFun instanceof JavaCallableMemberDescriptor)) {
                generateDelegationToTraitImpl(traitFun, entry.getValue());
            }
        }
    }

    private void generateDelegationToTraitImpl(@NotNull final FunctionDescriptor traitFun, @NotNull final FunctionDescriptor inheritedFun) {
        functionCodegen.generateMethod(
                JvmDeclarationOriginKt.DelegationToTraitImpl(descriptorToDeclaration(traitFun), traitFun),
                inheritedFun,
                new FunctionGenerationStrategy.CodegenBased<FunctionDescriptor>(state, inheritedFun) {
                    @Override
                    public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                        DeclarationDescriptor containingDeclaration = traitFun.getContainingDeclaration();
                        if (!DescriptorUtils.isInterface(containingDeclaration)) return;

                        DeclarationDescriptor declarationInheritedFun = inheritedFun.getContainingDeclaration();
                        PsiElement classForInheritedFun = descriptorToDeclaration(declarationInheritedFun);
                        if (classForInheritedFun instanceof KtDeclaration) {
                            codegen.markLineNumber((KtElement) classForInheritedFun, false);
                        }

                        ClassDescriptor containingTrait = (ClassDescriptor) containingDeclaration;
                        Type traitImplType = typeMapper.mapDefaultImpls(containingTrait);

                        Method traitMethod = typeMapper.mapAsmMethod(traitFun.getOriginal(), OwnerKind.DEFAULT_IMPLS);

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
                            // A special hack for Cloneable: there's no kotlin/Cloneable$DefaultImpls class at runtime,
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

        CallableMethod delegateConstructorCallable = typeMapper.mapToCallableMethod(delegateConstructor, false);
        CallableMethod callable = typeMapper.mapToCallableMethod(constructorDescriptor, false);

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
                // constructor. We need to traverse our outer classes from the bottom up, to find the needed class. See innerExtendsOuter.kt
                ClassDescriptor outerForSuper = (ClassDescriptor) superConstructor.getContainingDeclaration().getContainingDeclaration();
                StackValue outer = codegen.generateThisOrOuter(outerForSuper, true, true);
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
            Type type = parameters.get(i).getAsmType();
            pushDefaultValueOnStack(type, iv);
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

        @Override
        protected void reorderArgumentsIfNeeded(@NotNull List<ArgumentAndDeclIndex> args) {

        }
    }

    private void generateEnumEntries() {
        if (descriptor.getKind() != ClassKind.ENUM_CLASS) return;

        List<KtEnumEntry> enumEntries = CollectionsKt.filterIsInstance(element.getDeclarations(), KtEnumEntry.class);

        for (KtEnumEntry enumEntry : enumEntries) {
            ClassDescriptor descriptor = getNotNull(bindingContext, BindingContext.CLASS, enumEntry);
            int isDeprecated = KotlinBuiltIns.isDeprecated(descriptor) ? ACC_DEPRECATED : 0;
            FieldVisitor fv = v.newField(JvmDeclarationOriginKt.OtherOrigin(enumEntry, descriptor), ACC_PUBLIC | ACC_ENUM | ACC_STATIC | ACC_FINAL | isDeprecated,
                                         descriptor.getName().asString(), classAsmType.getDescriptor(), null, null);
            AnnotationCodegen.forField(fv, typeMapper).genAnnotations(descriptor, null);
        }

        initializeEnumConstants(enumEntries);
    }

    private void initializeEnumConstants(@NotNull List<KtEnumEntry> enumEntries) {
        if (!state.getClassBuilderMode().generateBodies) return;

        ExpressionCodegen codegen = createOrGetClInitCodegen();
        InstructionAdapter iv = codegen.v;

        Type arrayAsmType = typeMapper.mapType(DescriptorUtilsKt.getBuiltIns(descriptor).getArrayType(INVARIANT, descriptor.getDefaultType()));
        v.newField(JvmDeclarationOriginKt.OtherOrigin(myClass), ACC_PRIVATE | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC, ENUM_VALUES_FIELD_NAME,
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

    private void initializeEnumConstant(@NotNull List<KtEnumEntry> enumEntries, int ordinal) {
        ExpressionCodegen codegen = createOrGetClInitCodegen();
        InstructionAdapter iv = codegen.v;
        KtEnumEntry enumEntry = enumEntries.get(ordinal);

        iv.dup();
        iv.iconst(ordinal);

        ClassDescriptor classDescriptor = getNotNull(bindingContext, BindingContext.CLASS, enumEntry);
        Type implClass = typeMapper.mapClass(classDescriptor);

        iv.anew(implClass);
        iv.dup();

        iv.aconst(enumEntry.getName());
        iv.iconst(ordinal);

        List<KtSuperTypeListEntry> delegationSpecifiers = enumEntry.getSuperTypeListEntries();
        if (delegationSpecifiers.size() == 1 && !enumEntryNeedSubclass(bindingContext, enumEntry)) {
            ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCallWithAssert(delegationSpecifiers.get(0), bindingContext);

            CallableMethod method = typeMapper.mapToCallableMethod((ConstructorDescriptor) resolvedCall.getResultingDescriptor(), false);

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
        for (KtSuperTypeListEntry specifier : myClass.getSuperTypeListEntries()) {
            if (specifier instanceof KtDelegatedSuperTypeEntry) {
                DelegationFieldsInfo.Field field = delegationFieldsInfo.getInfo((KtDelegatedSuperTypeEntry) specifier);
                generateDelegateField(field);
                KtExpression delegateExpression = ((KtDelegatedSuperTypeEntry) specifier).getDelegateExpression();
                KotlinType delegateExpressionType = delegateExpression != null ? bindingContext.getType(delegateExpression) : null;
                generateDelegates(getSuperClass(specifier), delegateExpressionType, field);
            }
        }
    }

    private void generateDelegateField(DelegationFieldsInfo.Field fieldInfo) {
        if (!fieldInfo.generateField) return;

        v.newField(JvmDeclarationOrigin.NO_ORIGIN, ACC_PRIVATE | ACC_FINAL | ACC_SYNTHETIC,
                   fieldInfo.name, fieldInfo.type.getDescriptor(), /*TODO*/null, null);
    }

    private void generateDelegates(ClassDescriptor toTrait, KotlinType delegateExpressionType, DelegationFieldsInfo.Field field) {
        for (Map.Entry<CallableMemberDescriptor, CallableDescriptor> entry :
                CodegenUtil.getDelegates(descriptor, toTrait, delegateExpressionType).entrySet()) {
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
