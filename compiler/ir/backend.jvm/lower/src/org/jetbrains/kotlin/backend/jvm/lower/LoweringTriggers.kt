/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.ArrayConstructorLowering
import org.jetbrains.kotlin.backend.common.lower.RangeContainsLowering
import org.jetbrains.kotlin.backend.common.phaser.LoweringPhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClass
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isNumber
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.EnumSet

internal enum class LoweringTrigger(vararg val loweringClasses: Class<out FileLoweringPass>) {
    ANNOTATION_INSTANTIATION(JvmAnnotationImplementationLowering::class.java),
    POLYMORPHIC_SIGNATURE_CALL(PolymorphicSignatureLowering::class.java),
    LATEINIT(JvmLateinitLowering::class.java),
    DIRECT_INVOKE(DirectInvokeLowering::class.java, JvmReturnableBlockLowering::class.java),
    SUSPEND(SuspendLambdaLowering::class.java, TailCallOptimizationLowering::class.java, AddContinuationLowering::class.java),
    OPTIMIZABLE_DELEGATED_PROPERTY(
        PropertyReferenceDelegationLowering::class.java,
        SingletonOrConstantDelegationLowering::class.java,
        JvmReturnableBlockLowering::class.java,
        MakePropertyDelegateMethodsStaticLowering::class.java,
    ),
    ARRAY_CONSTRUCTOR_CALL(ArrayConstructorLowering::class.java, JvmReturnableBlockLowering::class.java),
    OBJECT_LITERAL(AnonymousObjectSuperConstructorLowering::class.java),
    IN_CALL(RangeContainsLowering::class.java),
    COLLECTION_SUBCLASS(CollectionStubMethodLowering::class.java, ToArrayLowering::class.java),
    SAM_CONVERSION(JvmSingleAbstractMethodLowering::class.java),
    TAILREC(JvmTailrecCheckerLowering::class.java, JvmTailrecLowering::class.java),
    ASSERT_CALL(JvmReturnableBlockLowering::class.java),
    RETURNABLE_BLOCK(JvmReturnableBlockLowering::class.java),
    ALL_DEFAULT_PRIMARY_CONSTRUCTOR(JvmDefaultConstructorLowering::class.java),
    INTERFACE_OR_ANNOTATION_CLASS(
        JvmInterfacePrivateFieldsLowering::class.java,
        InterfaceLowering::class.java,
        AdditionalClassAnnotationLowering::class.java,
    ),
    INTERFACE_SUPER_CALL(InterfaceSuperCallsLowering::class.java),
    INNER_CLASS(
        JvmInnerClassesLowering::class.java,
        JvmInnerClassesMemberBodyLowering::class.java,
        JvmInnerClassConstructorCallsLowering::class.java,
    ),
    ENUM_CLASS(EnumClassLowering::class.java),
    ENUM_ENTRIES_CALL(EnumExternalEntriesLowering::class.java),
    JVM_STATIC(JvmStaticInCompanionLowering::class.java),
    KFUNCTION_INVOKE(ReplaceKFunctionInvokeWithFunctionInvoke::class.java),
    NUMBER_TO_CHAR_CALL(ReplaceNumberToCharCallSitesLowering::class.java),
}

private var IrFile.loweringTriggers: Set<LoweringTrigger>? by irAttribute(copyByDefault = false)

private val CLONE_NAME = Name.identifier("clone")

internal class CollectLoweringTriggers(private val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val collector = LoweringTriggerCollector(context.irBuiltIns)
        irFile.acceptChildrenVoid(collector)
        irFile.loweringTriggers = collector.triggers
    }
}

private class LoweringTriggerCollector(irBuiltIns: IrBuiltIns) : IrVisitorVoid() {
    val triggers: EnumSet<LoweringTrigger> = EnumSet.noneOf(LoweringTrigger::class.java)

    private val arrayClass = irBuiltIns.arrayClass
    private val primitiveArrayClasses = irBuiltIns.primitiveArraysToPrimitiveTypes.keys
    private val readOnlyCollectionClasses =
        setOf(irBuiltIns.iterableClass, irBuiltIns.iteratorClass, irBuiltIns.mapClass, irBuiltIns.mapEntryClass)
    private val collectionSubtypes = hashMapOf<IrClass, Boolean>()
    private val classesWithNonAbstractMembers = hashMapOf<IrClass, Boolean>()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        when (declaration.kind) {
            ClassKind.INTERFACE -> {
                triggers += LoweringTrigger.INTERFACE_OR_ANNOTATION_CLASS
                triggers += LoweringTrigger.INTERFACE_SUPER_CALL
            }
            ClassKind.ANNOTATION_CLASS -> triggers += LoweringTrigger.INTERFACE_OR_ANNOTATION_CLASS
            ClassKind.ENUM_CLASS -> triggers += LoweringTrigger.ENUM_CLASS
            else -> {}
        }
        if (declaration.isInner) triggers += LoweringTrigger.INNER_CLASS
        if (!declaration.isInterface && declaration.isCollectionSubtype()) triggers += LoweringTrigger.COLLECTION_SUBCLASS
        if (declaration.mayGetDefaultConstructor()) triggers += LoweringTrigger.ALL_DEFAULT_PRIMARY_CONSTRUCTOR
        super.visitClass(declaration)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        if (declaration.isSuspend) triggers += LoweringTrigger.SUSPEND
        if (declaration.isTailrec) triggers += LoweringTrigger.TAILREC
        if (declaration.isFakeOverride && declaration.mayInheritInterfaceImplementation()) {
            triggers += LoweringTrigger.INTERFACE_SUPER_CALL
        }
        if (declaration.hasAnnotation(JvmStandardClassIds.Annotations.JvmStatic)) triggers += LoweringTrigger.JVM_STATIC
        super.visitSimpleFunction(declaration)
    }

    override fun visitProperty(declaration: IrProperty) {
        if (declaration.isLateinit && !declaration.isFakeOverride) triggers += LoweringTrigger.LATEINIT
        if (declaration.getRichPropertyReferenceForOptimizableDelegatedProperty() != null ||
            declaration.getSingletonOrConstantForOptimizableDelegatedProperty() != null
        ) {
            triggers += LoweringTrigger.OPTIMIZABLE_DELEGATED_PROPERTY
        }
        if (declaration.hasAnnotation(JvmStandardClassIds.Annotations.JvmStatic)) triggers += LoweringTrigger.JVM_STATIC
        super.visitProperty(declaration)
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) {
        if (declaration.delegate?.initializer is IrRichPropertyReference) triggers += LoweringTrigger.OPTIMIZABLE_DELEGATED_PROPERTY
        super.visitLocalDelegatedProperty(declaration)
    }

    override fun visitVariable(declaration: IrVariable) {
        if (declaration.isLateinit) triggers += LoweringTrigger.LATEINIT
        super.visitVariable(declaration)
    }

    override fun visitGetField(expression: IrGetField) {
        if (expression.symbol.owner.correspondingPropertySymbol?.owner?.isLateinit == true) triggers += LoweringTrigger.LATEINIT
        super.visitGetField(expression)
    }

    override fun visitBlock(expression: IrBlock) {
        if (expression.origin == IrStatementOrigin.OBJECT_LITERAL) triggers += LoweringTrigger.OBJECT_LITERAL
        if (expression is IrReturnableBlock) triggers += LoweringTrigger.RETURNABLE_BLOCK
        super.visitBlock(expression)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        if (expression.operator == IrTypeOperator.SAM_CONVERSION) {
            triggers += LoweringTrigger.SAM_CONVERSION
            val samClass = expression.typeOperand.classOrNull?.owner
            if (samClass == null || samClass.functions.any { it.isSuspend }) triggers += LoweringTrigger.SUSPEND
            if (samClass == null || samClass.hasNonAbstractMembers()) triggers += LoweringTrigger.INTERFACE_SUPER_CALL
        }
        super.visitTypeOperator(expression)
    }

    override fun visitRichFunctionReference(expression: IrRichFunctionReference) {
        val samClass = expression.type.classOrNull?.owner
        if (samClass == null || samClass.isCollectionSubtype()) triggers += LoweringTrigger.COLLECTION_SUBCLASS
        if (samClass == null || samClass.hasNonAbstractMembers()) triggers += LoweringTrigger.INTERFACE_SUPER_CALL
        if (expression.boundValues.any { it.type.isKFunction() || it.type.isKSuspendFunction() }) {
            triggers += LoweringTrigger.KFUNCTION_INVOKE
        }
        super.visitRichFunctionReference(expression)
    }

    override fun visitRichPropertyReference(expression: IrRichPropertyReference) {
        if ((expression.reflectionTargetSymbol?.owner as? IrProperty)?.getter?.name == SpecialNames.ENUM_GET_ENTRIES) {
            triggers += LoweringTrigger.ENUM_ENTRIES_CALL
        }
        super.visitRichPropertyReference(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference) {
        recordReferencedFunction(expression.symbol.owner)
        super.visitFunctionReference(expression)
    }

    override fun visitRawFunctionReference(expression: IrRawFunctionReference) {
        recordReferencedFunction(expression.symbol.owner)
        super.visitRawFunctionReference(expression)
    }

    override fun visitConstructorCall(expression: IrConstructorCall) {
        if (expression.type.classOrNull?.owner?.isAnnotationClass == true) triggers += LoweringTrigger.ANNOTATION_INSTANTIATION
        if (expression.isArrayConstructorCallWithInitializer()) triggers += LoweringTrigger.ARRAY_CONSTRUCTOR_CALL
        recordReferencedFunction(expression.symbol.owner)
        super.visitConstructorCall(expression)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        recordReferencedFunction(expression.symbol.owner)
        super.visitDelegatingConstructorCall(expression)
    }

    override fun visitCall(expression: IrCall) {
        val callee = expression.symbol.owner
        if (expression.origin == IrStatementOrigin.IN || expression.origin == IrStatementOrigin.NOT_IN) {
            triggers += LoweringTrigger.IN_CALL
        }
        if (expression.isPolymorphicCall()) triggers += LoweringTrigger.POLYMORPHIC_SIGNATURE_CALL
        if (expression.superQualifierSymbol?.owner?.isInterface == true) triggers += LoweringTrigger.INTERFACE_SUPER_CALL
        if (callee.visibility == DescriptorVisibilities.PROTECTED && (callee.parent as? IrClass)?.isCompanion == true) {
            triggers += LoweringTrigger.JVM_STATIC
        }
        recordReferencedFunction(callee)
        when (callee.name) {
            OperatorNameConventions.INVOKE -> {
                if (expression.arguments.firstOrNull() is IrRichFunctionReference && expression.symbol.isFunctionalTypeInvoke) {
                    triggers += LoweringTrigger.DIRECT_INVOKE
                }
                if (expression.isKFunctionInvoke()) triggers += LoweringTrigger.KFUNCTION_INVOKE
            }
            OperatorNameConventions.TO_CHAR -> {
                if ((callee.resolveFakeOverride() ?: callee).dispatchReceiverParameter?.type?.isNumber() == true) {
                    triggers += LoweringTrigger.NUMBER_TO_CHAR_CALL
                }
            }
            SpecialNames.ENUM_GET_ENTRIES -> triggers += LoweringTrigger.ENUM_ENTRIES_CALL
        }
        when (callee.name.asString()) {
            "<get-isInitialized>" -> triggers += LoweringTrigger.LATEINIT
            "assert" -> triggers += LoweringTrigger.ASSERT_CALL
        }
        super.visitCall(expression)
    }

    private fun recordReferencedFunction(function: IrFunction) {
        if (function.isSuspend) triggers += LoweringTrigger.SUSPEND
        if (function is IrConstructor && function.constructedClass.isInner) triggers += LoweringTrigger.INNER_CLASS
    }

    private fun IrConstructorCall.isArrayConstructorCallWithInitializer(): Boolean {
        val constructor = symbol.owner
        if (constructor.parameters.size != 2) return false
        val constructedClass = constructor.constructedClass.symbol
        return constructedClass == arrayClass || constructedClass in primitiveArrayClasses
    }

    private fun IrCall.isKFunctionInvoke(): Boolean {
        val calleeClass = symbol.owner.parentClassOrNull?.symbol
        if (calleeClass != null && (calleeClass.isKFunction() || calleeClass.isKSuspendFunction())) return true
        val receiverType = arguments.firstOrNull()?.type ?: return false
        return receiverType.isKFunction() || receiverType.isKSuspendFunction()
    }

    private fun IrClass.isCollectionSubtype(): Boolean =
        collectionSubtypes.getOrPut(this) {
            symbol in readOnlyCollectionClasses || superTypes.any { it.classOrNull?.owner?.isCollectionSubtype() == true }
        }

    private fun IrClass.hasNonAbstractMembers(): Boolean =
        classesWithNonAbstractMembers.getOrPut(this) {
            declarations.any { member ->
                when (member) {
                    is IrSimpleFunction -> member.modality != Modality.ABSTRACT && !member.isMethodOfAny()
                    is IrProperty -> listOfNotNull(member.getter, member.setter).any { it.modality != Modality.ABSTRACT }
                    else -> false
                }
            }
        }

    private fun IrSimpleFunction.mayInheritInterfaceImplementation(): Boolean =
        name == CLONE_NAME || !isMethodOfAny() && overriddenSymbols.any {
            it.owner.modality != Modality.ABSTRACT && it.owner.parentClassOrNull?.isInterface == true
        }

    private fun IrClass.mayGetDefaultConstructor(): Boolean {
        if (kind != ClassKind.CLASS || isInlineClass || isInner || modality == Modality.SEALED) return false
        val primaryConstructor = primaryConstructor ?: return false
        return !DescriptorVisibilities.isPrivate(primaryConstructor.visibility) &&
                primaryConstructor.parameters.isNotEmpty() &&
                primaryConstructor.parameters.all { it.hasDefaultValue() }
    }
}

internal fun List<NamedCompilerPhase<JvmBackendContext, IrFile, IrFile>>.gatedByLoweringTriggers(): List<NamedCompilerPhase<JvmBackendContext, IrFile, IrFile>> =
    map { phase ->
        @Suppress("UNCHECKED_CAST")
        val loweringPhase = phase as? LoweringPhase<JvmBackendContext, IrFile, FileLoweringPass> ?: return@map phase
        val triggers = LoweringTrigger.entries.filterTo(EnumSet.noneOf(LoweringTrigger::class.java)) {
            loweringPhase.loweringClass in it.loweringClasses
        }
        if (triggers.isEmpty()) phase else TriggerGatedFileLoweringPhase(loweringPhase, triggers)
    }

private class TriggerGatedFileLoweringPhase(
    phase: LoweringPhase<JvmBackendContext, IrFile, FileLoweringPass>,
    private val triggers: Set<LoweringTrigger>,
) : LoweringPhase<JvmBackendContext, IrFile, FileLoweringPass>(phase.loweringClass, phase.createLoweringPass) {
    override fun phaseBody(context: JvmBackendContext, input: IrFile): IrFile {
        val fileTriggers = input.loweringTriggers
        if (fileTriggers == null || triggers.any { it in fileTriggers }) {
            createLoweringPass(context).lower(input)
        }
        return input
    }
}
