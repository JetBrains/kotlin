/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext
import org.jetbrains.kotlin.codegen.context.MultifileClassFacadeContext
import org.jetbrains.kotlin.codegen.coroutines.continuationAsmType
import org.jetbrains.kotlin.codegen.coroutines.unwrapInitialDescriptorForSuspendFunction
import org.jetbrains.kotlin.codegen.inline.NUMBERED_FUNCTION_PREFIX
import org.jetbrains.kotlin.codegen.inline.ReificationArgument
import org.jetbrains.kotlin.codegen.intrinsics.TypeIntrinsics
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.deserialization.PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.SpecialSignatureInfo
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.DescriptorUtils.isSubclass
import org.jetbrains.kotlin.resolve.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.resolve.calls.callUtil.getFirstArgumentExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.Synthetic
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor.CoroutinesCompatibilityMode
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.util.Textifier
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

fun generateIsCheck(
    v: InstructionAdapter,
    kotlinType: KotlinType,
    asmType: Type,
    isReleaseCoroutines: Boolean
) {
    if (TypeUtils.isNullableType(kotlinType)) {
        val nope = Label()
        val end = Label()

        with(v) {
            dup()

            ifnull(nope)

            TypeIntrinsics.instanceOf(this, kotlinType, asmType, isReleaseCoroutines)

            goTo(end)

            mark(nope)
            pop()
            iconst(1)

            mark(end)
        }
    } else {
        TypeIntrinsics.instanceOf(v, kotlinType, asmType, isReleaseCoroutines)
    }
}

fun generateAsCast(
    v: InstructionAdapter,
    kotlinType: KotlinType,
    asmType: Type,
    isSafe: Boolean,
    languageVersionSettings: LanguageVersionSettings
) {
    if (!isSafe) {
        if (!TypeUtils.isNullableType(kotlinType)) {
            generateNullCheckForNonSafeAs(v, kotlinType, languageVersionSettings)
        }
    } else {
        with(v) {
            dup()
            TypeIntrinsics.instanceOf(v, kotlinType, asmType, languageVersionSettings.isReleaseCoroutines())
            val ok = Label()
            ifne(ok)
            pop()
            aconst(null)
            mark(ok)
        }
    }

    TypeIntrinsics.checkcast(v, kotlinType, asmType, isSafe)
}

private fun generateNullCheckForNonSafeAs(
    v: InstructionAdapter,
    type: KotlinType,
    languageVersionSettings: LanguageVersionSettings
) {
    with(v) {
        dup()
        val nonnull = Label()
        ifnonnull(nonnull)
        val exceptionClass =
            if (languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_4) "java/lang/NullPointerException"
            else "kotlin/TypeCastException"
        AsmUtil.genThrow(
            v,
            exceptionClass,
            "null cannot be cast to non-null type " + DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type)
        )
        mark(nonnull)
    }
}

fun SpecialSignatureInfo.replaceValueParametersIn(sourceSignature: String?): String? =
    valueParametersSignature?.let { sourceSignature?.replace("^\\(.*\\)".toRegex(), "($it)") }

fun populateCompanionBackingFieldNamesToOuterContextIfNeeded(
    companion: KtObjectDeclaration,
    outerContext: FieldOwnerContext<*>,
    state: GenerationState
) {
    val descriptor = state.bindingContext.get(BindingContext.CLASS, companion)

    if (descriptor == null || ErrorUtils.isError(descriptor)) {
        return
    }

    if (!JvmAbi.isClassCompanionObjectWithBackingFieldsInOuter(descriptor)) {
        return
    }
    val properties = companion.declarations.filterIsInstance<KtProperty>()

    properties.forEach {
        val variableDescriptor = state.bindingContext.get(BindingContext.VARIABLE, it)
        if (variableDescriptor is PropertyDescriptor) {
            outerContext.getFieldName(variableDescriptor, it.hasDelegate())
        }
    }

}

// Top level subclasses of a sealed class should be generated before that sealed class,
// so that we'd generate the necessary accessor for its constructor afterwards
fun sortTopLevelClassesAndPrepareContextForSealedClasses(
    classOrObjects: List<KtClassOrObject>,
    context: CodegenContext<*>,
    state: GenerationState
): List<KtClassOrObject> {
    fun prepareContextIfNeeded(descriptor: ClassDescriptor?) {
        if (DescriptorUtils.isSealedClass(descriptor)) {
            // save context for sealed class
            context.intoClass(descriptor!!, OwnerKind.IMPLEMENTATION, state)
        }
    }

    // optimization
    when (classOrObjects.size) {
        0 -> return emptyList()
        1 -> {
            prepareContextIfNeeded(state.bindingContext.get(BindingContext.CLASS, classOrObjects.first()))
            return classOrObjects
        }
    }

    val result = ArrayList<KtClassOrObject>(classOrObjects.size)
    val descriptorToPsi = LinkedHashMap<ClassDescriptor, KtClassOrObject>()
    for (classOrObject in classOrObjects) {
        val descriptor = state.bindingContext.get(BindingContext.CLASS, classOrObject)
        if (descriptor == null) {
            result.add(classOrObject)
        } else {
            prepareContextIfNeeded(descriptor)
            descriptorToPsi[descriptor] = classOrObject
        }
    }

    // topologicalOrder(listOf(1, 2, 3)) { emptyList() } = listOf(3, 2, 1). Because of this used keys.reversed().
    val sortedDescriptors = DFS.topologicalOrder(descriptorToPsi.keys.reversed()) { descriptor ->
        descriptor.typeConstructor.supertypes
            .map { it.constructor.declarationDescriptor as? ClassDescriptor }
            .filter { it in descriptorToPsi.keys }
    }
    sortedDescriptors.mapTo(result) { descriptorToPsi[it]!! }
    return result
}

fun CallableMemberDescriptor.isDefinitelyNotDefaultImplsMethod() =
    this is JavaCallableMemberDescriptor || this.annotations.hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME)


fun ClassBuilder.generateMethod(
    debugString: String,
    access: Int,
    method: Method,
    element: PsiElement?,
    origin: JvmDeclarationOrigin,
    state: GenerationState,
    generate: InstructionAdapter.() -> Unit
) {
    val mv = this.newMethod(origin, access, method.name, method.descriptor, null, null)

    if (state.classBuilderMode.generateBodies) {
        val iv = InstructionAdapter(mv)
        iv.visitCode()
        iv.generate()
        iv.areturn(method.returnType)
        FunctionCodegen.endVisit(mv, debugString, element)
    }
}

fun CallableDescriptor.isJvmStaticInObjectOrClassOrInterface(): Boolean =
    isJvmStaticIn {
        DescriptorUtils.isNonCompanionObject(it) ||
                // This is necessary because for generation of @JvmStatic methods from companion of class A
                // we create a synthesized descriptor containing in class A
                DescriptorUtils.isClassOrEnumClass(it) || DescriptorUtils.isInterface(it)
    }

fun CallableDescriptor.isJvmStaticInCompanionObject(): Boolean =
    isJvmStaticIn { DescriptorUtils.isCompanionObject(it) }

fun CallableDescriptor.isJvmStaticInInlineClass(): Boolean =
    isJvmStaticIn { it.isInlineClass() }

private fun CallableDescriptor.isJvmStaticIn(predicate: (DeclarationDescriptor) -> Boolean): Boolean =
    when (this) {
        is PropertyAccessorDescriptor -> {
            val propertyDescriptor = correspondingProperty
            predicate(propertyDescriptor.containingDeclaration) &&
                    (hasJvmStaticAnnotation() || propertyDescriptor.hasJvmStaticAnnotation())
        }
        else -> predicate(containingDeclaration) && hasJvmStaticAnnotation()
    }

fun Collection<VariableDescriptor>.filterOutDescriptorsWithSpecialNames() = filterNot { it.name.isSpecial }

class JvmKotlinType(val type: Type, val kotlinType: KotlinType? = null)

fun KotlinType.asmType(typeMapper: KotlinTypeMapper) = typeMapper.mapType(this)

fun KtExpression?.asmType(typeMapper: KotlinTypeMapper, bindingContext: BindingContext): Type =
    this.kotlinType(bindingContext)?.asmType(typeMapper) ?: Type.VOID_TYPE

fun KtExpression?.kotlinType(bindingContext: BindingContext) = this?.let(bindingContext::getType)

fun Collection<Type>.withVariableIndices(): List<Pair<Int, Type>> = mutableListOf<Pair<Int, Type>>().apply {
    var index = 0
    for (type in this@withVariableIndices) {
        add(index to type)
        index += type.size
    }
}

fun FunctionDescriptor.isGenericToArray(): Boolean {
    if (name.asString() != "toArray") return false
    if (valueParameters.size != 1 || typeParameters.size != 1) return false

    val returnType = returnType ?: throw AssertionError(toString())
    val paramType = valueParameters[0].type

    if (!KotlinBuiltIns.isArray(returnType) || !KotlinBuiltIns.isArray(paramType)) return false

    val elementType = typeParameters[0].defaultType
    return KotlinTypeChecker.DEFAULT.equalTypes(elementType, builtIns.getArrayElementType(returnType)) &&
            KotlinTypeChecker.DEFAULT.equalTypes(elementType, builtIns.getArrayElementType(paramType))
}

fun FunctionDescriptor.isNonGenericToArray(): Boolean {
    if (name.asString() != "toArray") return false
    if (valueParameters.isNotEmpty() || typeParameters.isNotEmpty()) return false

    val returnType = returnType
    return returnType != null && KotlinBuiltIns.isArray(returnType)
}

fun MemberDescriptor.isToArrayFromCollection(): Boolean {
    if (this !is FunctionDescriptor) return false

    val containingClassDescriptor = containingDeclaration as? ClassDescriptor ?: return false
    if (containingClassDescriptor.source == SourceElement.NO_SOURCE) return false

    val collectionClass = builtIns.collection
    if (!isSubclass(containingClassDescriptor, collectionClass)) return false

    return isGenericToArray() || isNonGenericToArray()
}

fun FqName.topLevelClassInternalName() = JvmClassName.byClassId(ClassId(parent(), shortName())).internalName
fun FqName.topLevelClassAsmType(): Type = Type.getObjectType(topLevelClassInternalName())

fun initializeVariablesForDestructuredLambdaParameters(codegen: ExpressionCodegen, valueParameters: List<ValueParameterDescriptor>, endLabel: Label? = null) {
    // Do not write line numbers until destructuring happens
    // (otherwise destructuring variables will be uninitialized in the beginning of lambda)
    codegen.runWithShouldMarkLineNumbers(false) {
        for (parameterDescriptor in valueParameters) {
            if (parameterDescriptor !is ValueParameterDescriptorImpl.WithDestructuringDeclaration) continue

            val variables = parameterDescriptor.destructuringVariables.filterOutDescriptorsWithSpecialNames()
            val indices = variables.map {
                codegen.myFrameMap.enter(it, codegen.typeMapper.mapType(it.type))
            }

            val destructuringDeclaration =
                (DescriptorToSourceUtils.descriptorToDeclaration(parameterDescriptor) as? KtParameter)?.destructuringDeclaration
                    ?: error("Destructuring declaration for descriptor $parameterDescriptor not found")

            codegen.initializeDestructuringDeclarationVariables(
                destructuringDeclaration,
                TransientReceiver(parameterDescriptor.type),
                codegen.findLocalOrCapturedValue(parameterDescriptor) ?: error("Local var not found for parameter $parameterDescriptor")
            )

            if (endLabel != null) {
                val label = Label()
                codegen.v.mark(label)
                for ((index, entry) in indices.zip(variables)) {
                    codegen.v.visitLocalVariable(
                        entry.name.asString(),
                        codegen.typeMapper.mapType(entry.type).descriptor,
                        null,
                        label,
                        endLabel,
                        index
                    )
                }
            }
        }
    }
}

fun <D : CallableDescriptor> D.unwrapFrontendVersion() = unwrapInitialDescriptorForSuspendFunction()

inline fun FrameMap.useTmpVar(type: Type, block: (index: Int) -> Unit) {
    val index = enterTemp(type)
    block(index)
    leaveTemp(type)
}

fun InstructionAdapter.generateNewInstanceDupAndPlaceBeforeStackTop(
    frameMap: FrameMap,
    topStackType: Type,
    newInstanceInternalName: String
) {
    frameMap.useTmpVar(topStackType) { index ->
        store(index, topStackType)
        anew(Type.getObjectType(newInstanceInternalName))
        dup()
        load(index, topStackType)
    }
}

fun extractReificationArgument(initialType: KotlinType): Pair<TypeParameterDescriptor, ReificationArgument>? {
    var type = initialType
    var arrayDepth = 0
    val isNullable = type.isMarkedNullable
    while (KotlinBuiltIns.isArray(type)) {
        arrayDepth++
        type = type.arguments[0].type
    }

    val parameterDescriptor = TypeUtils.getTypeParameterDescriptorOrNull(type) ?: return null

    return Pair(parameterDescriptor, ReificationArgument(parameterDescriptor.name.asString(), isNullable, arrayDepth))
}

fun unwrapInitialSignatureDescriptor(function: FunctionDescriptor): FunctionDescriptor =
    function.initialSignatureDescriptor ?: function

fun ExpressionCodegen.generateCallReceiver(call: ResolvedCall<out CallableDescriptor>): StackValue =
    generateReceiverValue(call.extensionReceiver ?: call.dispatchReceiver!!, false)

fun ExpressionCodegen.generateCallSingleArgument(call: ResolvedCall<out CallableDescriptor>): StackValue =
    gen(call.getFirstArgumentExpression()!!)

fun ClassDescriptor.isPossiblyUninitializedSingleton() =
    DescriptorUtils.isEnumEntry(this) ||
            DescriptorUtils.isCompanionObject(this) && JvmCodegenUtil.isJvmInterface(this.containingDeclaration)

inline fun FrameMap.evaluateOnce(
    value: StackValue,
    asType: Type,
    v: InstructionAdapter,
    body: (StackValue) -> Unit
) {
    val valueOrTmp: StackValue =
        if (value.canHaveSideEffects())
            StackValue.local(enterTemp(asType), asType).apply { store(value, v) }
        else
            value

    body(valueOrTmp)

    if (valueOrTmp != value) {
        leaveTemp(asType)
    }
}

// Handy debugging routine. Print all instructions from methodNode.
@TestOnly
@Suppress("unused")
fun MethodNode.textifyMethodNode(): String {
    val text = Textifier()
    val tmv = TraceMethodVisitor(text)
    this.instructions.asSequence().forEach { it.accept(tmv) }
    localVariables.forEach { text.visitLocalVariable(it.name, it.desc, it.signature, it.start.label, it.end.label, it.index) }
    val sw = StringWriter()
    text.print(PrintWriter(sw))
    return "$sw"
}

fun KotlinType.isInlineClassTypeWithPrimitiveEquality(): Boolean {
    if (!isInlineClassType()) return false

    // Always treat unsigned types as inline classes with primitive equality
    if (UnsignedTypes.isUnsignedType(this)) return true

    // TODO support other inline classes that can be compared as underlying primitives
    return false
}

fun CallableDescriptor.needsExperimentalCoroutinesWrapper() =
    (this as? DeserializedMemberDescriptor)?.coroutinesExperimentalCompatibilityMode == CoroutinesCompatibilityMode.NEEDS_WRAPPER

fun recordCallLabelForLambdaArgument(declaration: KtFunctionLiteral, bindingTrace: BindingTrace) {
    val labelName = getCallLabelForLambdaArgument(declaration, bindingTrace.bindingContext) ?: return
    val functionDescriptor = bindingTrace[BindingContext.FUNCTION, declaration] ?: return
    bindingTrace.record(CodegenBinding.CALL_LABEL_FOR_LAMBDA_ARGUMENT, functionDescriptor, labelName)

}

fun getCallLabelForLambdaArgument(declaration: KtFunctionLiteral, bindingContext: BindingContext): String? {
    val lambdaExpression = declaration.parent as? KtLambdaExpression ?: return null
    val lambdaExpressionParent = lambdaExpression.parent

    if (lambdaExpressionParent is KtLabeledExpression) {
        lambdaExpressionParent.name?.let { return it }
    }

    val lambdaArgument = lambdaExpression.parent as? KtLambdaArgument ?: return null
    val callExpression = lambdaArgument.parent as? KtCallExpression ?: return null
    val call = callExpression.getResolvedCall(bindingContext) ?: return null

    return call.resultingDescriptor.name.asString()
}

private val ARRAY_OF_STRINGS_TYPE = Type.getType("[Ljava/lang/String;")
private val METHOD_DESCRIPTOR_FOR_MAIN = Type.getMethodDescriptor(Type.VOID_TYPE, ARRAY_OF_STRINGS_TYPE)

fun generateBridgeForMainFunctionIfNecessary(
    state: GenerationState,
    packagePartClassBuilder: ClassBuilder,
    functionDescriptor: FunctionDescriptor,
    signatureOfRealDeclaration: JvmMethodGenericSignature,
    origin: JvmDeclarationOrigin,
    parentContext: CodegenContext<*>
) {
    val originElement = origin.element ?: return
    if (functionDescriptor.name.asString() != "main" || !DescriptorUtils.isTopLevelDeclaration(functionDescriptor)) return

    val unwrappedFunctionDescriptor = functionDescriptor.unwrapInitialDescriptorForSuspendFunction()
    val isParameterless =
        unwrappedFunctionDescriptor.extensionReceiverParameter == null && unwrappedFunctionDescriptor.valueParameters.isEmpty()

    if (!functionDescriptor.isSuspend && !isParameterless) return
    if (!state.mainFunctionDetector.isMain(unwrappedFunctionDescriptor, checkJvmStaticAnnotation = false, checkReturnType = true)) return

    val bridgeMethodVisitor = packagePartClassBuilder.newMethod(
        Synthetic(originElement, functionDescriptor),
        ACC_PUBLIC or ACC_STATIC or ACC_SYNTHETIC,
        "main",
        METHOD_DESCRIPTOR_FOR_MAIN, null, null
    )

    if (parentContext is MultifileClassFacadeContext) {
        bridgeMethodVisitor.visitCode()
        FunctionCodegen.generateFacadeDelegateMethodBody(
            bridgeMethodVisitor,
            Method("main", METHOD_DESCRIPTOR_FOR_MAIN),
            parentContext
        )
        bridgeMethodVisitor.visitEnd()
        return
    }

    val lambdaInternalName =
        if (functionDescriptor.isSuspend)
            generateLambdaForRunSuspend(
                state,
                originElement,
                packagePartClassBuilder.thisName,
                signatureOfRealDeclaration,
                isParameterless
            )
        else
            null

    bridgeMethodVisitor.apply {
        visitCode()

        if (lambdaInternalName != null) {
            visitTypeInsn(NEW, lambdaInternalName)
            visitInsn(DUP)
            visitVarInsn(ALOAD, 0)
            visitMethodInsn(
                INVOKESPECIAL,
                lambdaInternalName,
                "<init>",
                METHOD_DESCRIPTOR_FOR_MAIN,
                false
            )

            visitMethodInsn(
                INVOKESTATIC,
                "kotlin/coroutines/jvm/internal/RunSuspendKt", "runSuspend",
                Type.getMethodDescriptor(
                    Type.VOID_TYPE,
                    Type.getObjectType(NUMBERED_FUNCTION_PREFIX + "1")
                ),
                false
            )
        } else {
            visitMethodInsn(
                INVOKESTATIC,
                packagePartClassBuilder.thisName, "main",
                Type.getMethodDescriptor(
                    Type.VOID_TYPE
                ),
                false
            )
        }

        visitInsn(RETURN)
        visitEnd()
    }
}

private fun generateLambdaForRunSuspend(
    state: GenerationState,
    originElement: PsiElement,
    packagePartClassInternalName: String,
    signatureOfRealDeclaration: JvmMethodGenericSignature,
    parameterless: Boolean
): String {
    val internalName = "$packagePartClassInternalName$$\$main"
    val lambdaBuilder = state.factory.newVisitor(
        JvmDeclarationOrigin.NO_ORIGIN,
        Type.getObjectType(internalName),
        originElement.containingFile
    )

    lambdaBuilder.defineClass(
        originElement, state.classFileVersion,
        ACC_FINAL or ACC_SUPER or ACC_SYNTHETIC,
        internalName, null,
        AsmTypes.LAMBDA.internalName,
        arrayOf(NUMBERED_FUNCTION_PREFIX + "1")
    )

    lambdaBuilder.newField(
        JvmDeclarationOrigin.NO_ORIGIN,
        ACC_PRIVATE or ACC_FINAL,
        "args",
        ARRAY_OF_STRINGS_TYPE.descriptor, null, null
    )

    lambdaBuilder.newMethod(
        JvmDeclarationOrigin.NO_ORIGIN,
        AsmUtil.NO_FLAG_PACKAGE_PRIVATE or ACC_SYNTHETIC,
        "<init>",
        METHOD_DESCRIPTOR_FOR_MAIN, null, null
    ).apply {
        visitCode()
        visitVarInsn(ALOAD, 0)
        visitVarInsn(ALOAD, 1)
        visitFieldInsn(
            PUTFIELD,
            lambdaBuilder.thisName,
            "args",
            ARRAY_OF_STRINGS_TYPE.descriptor
        )

        visitVarInsn(ALOAD, 0)
        visitInsn(ICONST_1)
        visitMethodInsn(
            INVOKESPECIAL,
            AsmTypes.LAMBDA.internalName,
            "<init>",
            Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE),
            false
        )
        visitInsn(RETURN)
        visitEnd()
    }

    lambdaBuilder.newMethod(
        JvmDeclarationOrigin.NO_ORIGIN,
        ACC_PUBLIC or ACC_FINAL or ACC_SYNTHETIC,
        "invoke",
        Type.getMethodDescriptor(AsmTypes.OBJECT_TYPE, AsmTypes.OBJECT_TYPE), null, null
    ).apply {
        visitCode()

        if (!parameterless) {
            // Actually, the field for arguments may also be removed in case of parameterless main,
            // but probably it'd much easier when IR is ready
            visitVarInsn(ALOAD, 0)
            visitFieldInsn(
                GETFIELD,
                lambdaBuilder.thisName,
                "args",
                ARRAY_OF_STRINGS_TYPE.descriptor
            )
        }

        visitVarInsn(ALOAD, 1)
        val continuationInternalName = state.languageVersionSettings.continuationAsmType().internalName

        visitTypeInsn(
            CHECKCAST,
            continuationInternalName
        )
        visitMethodInsn(
            INVOKESTATIC,
            packagePartClassInternalName,
            signatureOfRealDeclaration.asmMethod.name,
            signatureOfRealDeclaration.asmMethod.descriptor,
            false
        )
        visitInsn(ARETURN)
        visitEnd()
    }

    writeSyntheticClassMetadata(lambdaBuilder, state)

    lambdaBuilder.done()
    return lambdaBuilder.thisName
}
