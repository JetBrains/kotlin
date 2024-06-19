/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin.JVM_STATIC_WRAPPER
import org.jetbrains.kotlin.backend.jvm.JvmSyntheticAccessorGenerator
import org.jetbrains.kotlin.backend.jvm.ir.IrInlineScopeResolver
import org.jetbrains.kotlin.backend.jvm.ir.findInlineCallSites
import org.jetbrains.kotlin.backend.jvm.ir.isAssertionsDisabledField
import org.jetbrains.kotlin.backend.jvm.lower.SyntheticAccessorLowering.Companion.isAccessible
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.org.objectweb.asm.Opcodes

@PhaseDescription(
    name = "SyntheticAccessor",
    description = "Introduce synthetic accessors",
    prerequisite = [ObjectClassLowering::class, StaticDefaultFunctionLowering::class, InterfaceLowering::class]
)
internal class SyntheticAccessorLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val pendingAccessorsToAdd = mutableSetOf<IrFunction>()
        irFile.transformChildrenVoid(SyntheticAccessorTransformer(context, irFile, pendingAccessorsToAdd))
        for (accessor in pendingAccessorsToAdd) {
            (accessor.parent as IrDeclarationContainer).declarations.add(accessor)
        }
    }

    companion object {
        /**
         * Whether `this` is accessible in [currentScope], according to the platform rules, and with respect to function inlining.
         *
         * @param context The backend context.
         * @param currentScope The scope in which `this` is to be accessed.
         * @param inlineScopeResolver The helper that allows to find the places from which private inline functions are called (useful if
         *   `this` is accessed from a private inline function).
         * @param withSuper If an access to this symbol (like [IrCall]) has a `super` qualifier, the access rules will be stricter.
         * @param thisObjReference If this is a member access, the class symbol of the receiver.
         * @param fromOtherClassLoader If `this` is a protected declaration being accessed from the same package but not from a subclass,
         *   setting this parameter to `true` marks this declaration as inaccessible, since JVM `protected`, unlike Kotlin `protected`,
         *   permits accesses from the same package, _provided the call is not across class loader boundaries_.
         */
        fun IrSymbol.isAccessible(
            context: JvmBackendContext,
            currentScope: ScopeWithIr?,
            inlineScopeResolver: IrInlineScopeResolver,
            withSuper: Boolean, thisObjReference: IrClassSymbol?,
            fromOtherClassLoader: Boolean = false
        ): Boolean {
            /// We assume that IR code that reaches us has been checked for correctness at the frontend.
            /// This function needs to single out those cases where Java accessibility rules differ from Kotlin's.
            val declarationRaw = owner as IrDeclarationWithVisibility

            // Enum entry constructors are generated as package-private and are accessed only from corresponding enum class
            if (declarationRaw is IrConstructor && declarationRaw.constructedClass.isEnumEntry) return true

            // Public declarations are already accessible. However, `super` calls are subclass-only.
            val jvmVisibility = AsmUtil.getVisibilityAccessFlag(declarationRaw.visibility.delegate)
            if (jvmVisibility == Opcodes.ACC_PUBLIC && !withSuper) return true

            // `toArray` is always accessible cause mapped to public functions
            if (declarationRaw is IrSimpleFunction && (declarationRaw.isNonGenericToArray() || declarationRaw.isGenericToArray(context)) &&
                declarationRaw.parentAsClass.isCollectionSubClass
            ) return true

            // `$assertionsDisabled` is accessed only from the same class, even in an inline function
            // (the inliner will generate it at the call site if necessary).
            if (declarationRaw is IrField && declarationRaw.isAssertionsDisabledField(context)) return true

            // If this expression won't actually result in a JVM instruction call, access modifiers don't matter.
            if (declarationRaw is IrFunction && (declarationRaw.isInline || context.getIntrinsic(declarationRaw.symbol) != null))
                return true

            val declaration = when (declarationRaw) {
                is IrSimpleFunction -> declarationRaw.resolveFakeOverrideMaybeAbstractOrFail()
                is IrField -> declarationRaw.resolveFieldFakeOverride()
                else -> declarationRaw
            }

            val ownerClass = declaration.parent as? IrClass ?: return true // locals are always accessible
            val scopeClassOrPackage = inlineScopeResolver.findContainer(currentScope!!.irElement) ?: return false
            val samePackage = ownerClass.getPackageFragment().packageFqName == scopeClassOrPackage.getPackageFragment()?.packageFqName
            return when {
                jvmVisibility == Opcodes.ACC_PRIVATE -> ownerClass == scopeClassOrPackage
                !withSuper && samePackage && jvmVisibility == 0 /* package only */ -> true
                !withSuper && samePackage && !fromOtherClassLoader -> true
                // Super calls and cross-package protected accesses are both only possible from a subclass of the declaration
                // owner. Also, the target of a non-static call must be assignable to the current class. This is a verification
                // constraint: https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.10.1.8
                else -> (scopeClassOrPackage is IrClass && scopeClassOrPackage.isSubclassOf(ownerClass)) &&
                        (thisObjReference == null || thisObjReference.owner.isSubclassOf(scopeClassOrPackage))
            }
        }
    }
}

private class SyntheticAccessorTransformer(
    val context: JvmBackendContext,
    val irFile: IrFile,
    val pendingAccessorsToAdd: MutableSet<IrFunction>
) : IrElementTransformerVoidWithContext() {
    private val accessorGenerator = JvmSyntheticAccessorGenerator(context)
    private val inlineScopeResolver: IrInlineScopeResolver = irFile.findInlineCallSites(context)
    private var processingIrInlinedFun = false

    private inline fun <T> withinIrInlinedFun(block: () -> T): T {
        val oldProcessingInline = processingIrInlinedFun
        try {
            processingIrInlinedFun = true
            return block()
        } finally {
            processingIrInlinedFun = oldProcessingInline
        }
    }

    private fun <T : IrFunction> T.save(): T {
        assert(fileOrNull == irFile || processingIrInlinedFun) {
            "SyntheticAccessorLowering should not attempt to modify other files!\n" +
                    "While lowering this file: ${irFile.render()}\n" +
                    "Trying to add this accessor: ${render()}"
        }

        if (fileOrNull == irFile) {
            pendingAccessorsToAdd += this
        }
        return this
    }

    private fun IrSymbol.isAccessible(withSuper: Boolean, thisObjReference: IrClassSymbol?): Boolean =
        with(SyntheticAccessorLowering) {
            isAccessible(context, currentScope, inlineScopeResolver, withSuper, thisObjReference)
        }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        if (expression.usesDefaultArguments()) {
            return super.visitFunctionAccess(expression)
        }

        val callee = expression.symbol.owner
        val withSuper = (expression as? IrCall)?.superQualifierSymbol != null
        val thisSymbol = (expression as? IrCall)?.dispatchReceiver?.type?.classifierOrNull as? IrClassSymbol
        val generateSpecialAccessWithoutSyntheticAccessor =
            shouldGenerateSpecialAccessWithoutSyntheticAccessor(expression, withSuper, thisSymbol)

        if (expression is IrCall && callee.symbol == context.ir.symbols.indyLambdaMetafactoryIntrinsic) {
            return super.visitExpression(handleLambdaMetafactoryIntrinsic(expression, thisSymbol))
        }

        val accessor = when {
            generateSpecialAccessWithoutSyntheticAccessor -> return super.visitFunctionAccess(expression)
            callee is IrConstructor && accessorGenerator.isOrShouldBeHiddenAsSealedClassConstructor(callee) ->
                accessorGenerator.getSyntheticConstructorOfSealedClass(callee)
            callee is IrConstructor && accessorGenerator.isOrShouldBeHiddenSinceHasMangledParams(callee) ->
                accessorGenerator.getSyntheticConstructorWithMangledParams(callee)
            !expression.symbol.isAccessible(withSuper, thisSymbol) ->
                accessorGenerator.getSyntheticFunctionAccessor(expression, allScopes).save()

            else ->
                return super.visitFunctionAccess(expression)
        }
        return super.visitExpression(accessorGenerator.modifyFunctionAccessExpression(expression, accessor.symbol))
    }

    private fun shouldGenerateSpecialAccessWithoutSyntheticAccessor(
        expression: IrFunctionAccessExpression,
        withSuper: Boolean,
        thisObjReference: IrClassSymbol?,
    ): Boolean =
        when {
            context.evaluatorData == null -> false
            expression is IrCall -> {
                val inJvmStaticWrapper = (currentFunction?.irElement as? IrFunction)?.origin == JVM_STATIC_WRAPPER
                !inJvmStaticWrapper && !expression.symbol.isDirectlyAccessible(withSuper, thisObjReference)
            }
            expression is IrConstructorCall -> !expression.symbol.isDirectlyAccessible(withSuper = false, thisObjReference)
            else -> false
        }

    private fun shouldGenerateSpecialAccessWithoutSyntheticAccessor(symbol: IrSymbol): Boolean {
        return context.evaluatorData != null && !symbol.isDirectlyAccessible(withSuper = false, thisObjReference = null)
    }

    private fun IrSymbol.isDirectlyAccessible(withSuper: Boolean, thisObjReference: IrClassSymbol?): Boolean =
        isAccessible(context, currentScope, inlineScopeResolver, withSuper, thisObjReference, fromOtherClassLoader = true)

    private fun handleLambdaMetafactoryIntrinsic(call: IrCall, thisSymbol: IrClassSymbol?): IrExpression {
        val implFunRef = call.getValueArgument(1) as? IrFunctionReference
            ?: throw AssertionError("'implMethodReference' is expected to be 'IrFunctionReference': ${call.dump()}")
        val implFunSymbol = implFunRef.symbol

        if (implFunSymbol.isAccessibleFromSyntheticProxy(thisSymbol))
            return call

        val accessor = accessorGenerator.getSyntheticFunctionAccessor(implFunRef, allScopes).save()
        val accessorRef =
            IrFunctionReferenceImpl(
                implFunRef.startOffset, implFunRef.endOffset, implFunRef.type,
                accessor.symbol,
                accessor.typeParameters.size,
                accessor.valueParameters.size,
                implFunRef.reflectionTarget, implFunRef.origin
            )

        accessorRef.copyTypeArgumentsFrom(implFunRef)

        val implFun = implFunSymbol.owner
        var accessorArgIndex = 0
        if (implFun.dispatchReceiverParameter != null) {
            accessorRef.putValueArgument(accessorArgIndex++, implFunRef.dispatchReceiver)
        }
        if (implFun.extensionReceiverParameter != null) {
            accessorRef.putValueArgument(accessorArgIndex++, implFunRef.extensionReceiver)
        }
        for (implArgIndex in 0 until implFunRef.valueArgumentsCount) {
            accessorRef.putValueArgument(accessorArgIndex++, implFunRef.getValueArgument(implArgIndex))
        }
        if (accessor is IrConstructor) {
            accessorRef.putValueArgument(accessorArgIndex, accessorGenerator.createAccessorMarkerArgument())
        }

        call.putValueArgument(1, accessorRef)
        return call
    }

    private fun IrFunctionSymbol.isAccessibleFromSyntheticProxy(thisSymbol: IrClassSymbol?): Boolean {
        if (!isAccessible(false, thisSymbol))
            return false

        if (owner.visibility != DescriptorVisibilities.PROTECTED &&
            owner.visibility != JavaDescriptorVisibilities.PROTECTED_STATIC_VISIBILITY
        ) {
            return true
        }

        // We have a protected member.
        // It is accessible from a synthetic proxy class (created by LambdaMetafactory)
        // if it belongs to the current class.
        return inlineScopeResolver.findContainer(currentScope!!.irElement) == owner.parentAsClass
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        val dispatchReceiverType = expression.receiver?.type
        val dispatchReceiverClassSymbol = dispatchReceiverType?.classifierOrNull as? IrClassSymbol
        if (expression.symbol.isAccessible(withSuper = false, dispatchReceiverClassSymbol)) {
            return super.visitExpression(expression)
        }

        if (shouldGenerateSpecialAccessWithoutSyntheticAccessor(expression.symbol)) {
            return super.visitExpression(expression)
        }

        return super.visitExpression(
            modifyGetterExpression(
                expression, accessorGenerator.getSyntheticGetter(expression, allScopes).save().symbol
            )
        )
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        // FE accepts code that assigns to a val of this or other class if it happens in unreachable code (KT-35565).
        // Sometimes this can cause internal error in the BE (see KT-49316).
        // Assume that 'val' property with a backing field can never be initialized from a context that requires synthetic accessor.
        val correspondingProperty = expression.symbol.owner.correspondingPropertySymbol?.owner
        if (correspondingProperty != null && !correspondingProperty.isVar) {
            return super.visitExpression(expression)
        }

        if (shouldGenerateSpecialAccessWithoutSyntheticAccessor(expression.symbol)) {
            return super.visitExpression(expression)
        }

        val dispatchReceiverType = expression.receiver?.type
        val dispatchReceiverClassSymbol = dispatchReceiverType?.classifierOrNull as? IrClassSymbol
        if (expression.symbol.isAccessible(false, dispatchReceiverClassSymbol)) {
            return super.visitExpression(expression)
        }

        return super.visitExpression(
            modifySetterExpression(
                expression, accessorGenerator.getSyntheticSetter(expression, allScopes).save().symbol
            )
        )
    }

    override fun visitConstructor(declaration: IrConstructor): IrStatement {
        when {
            accessorGenerator.isOrShouldBeHiddenSinceHasMangledParams(declaration) -> {
                accessorGenerator.getSyntheticConstructorWithMangledParams(declaration).save()
                declaration.visibility = DescriptorVisibilities.PRIVATE
            }
            accessorGenerator.isOrShouldBeHiddenAsSealedClassConstructor(declaration) -> {
                accessorGenerator.getSyntheticConstructorOfSealedClass(declaration).save()
                declaration.visibility = DescriptorVisibilities.PRIVATE
            }
        }
        return super.visitConstructor(declaration)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        val function = expression.symbol.owner

        if (!expression.origin.isLambda && function is IrConstructor) {
            val generatedAccessor = when {
                accessorGenerator.isOrShouldBeHiddenSinceHasMangledParams(function) -> accessorGenerator.getSyntheticConstructorWithMangledParams(function)
                accessorGenerator.isOrShouldBeHiddenAsSealedClassConstructor(function) -> accessorGenerator.getSyntheticConstructorOfSealedClass(function)
                else -> return super.visitFunctionReference(expression)
            }
            expression.transformChildrenVoid()
            return IrFunctionReferenceImpl(
                expression.startOffset, expression.endOffset, expression.type,
                generatedAccessor.symbol, generatedAccessor.typeParameters.size,
                generatedAccessor.valueParameters.size, generatedAccessor.symbol, expression.origin
            )
        }

        return super.visitFunctionReference(expression)
    }

    override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock): IrExpression {
        if (inlinedBlock.isFunctionInlining()) {
            val callee = inlinedBlock.inlineDeclaration
            val parentClass = callee.parentClassOrNull ?: return super.visitInlinedFunctionBlock(inlinedBlock)
            return withinIrInlinedFun {
                withinScope(parentClass) {
                    withinScope(callee) {
                        super.visitInlinedFunctionBlock(inlinedBlock)
                    }
                }
            }
        }
        return super.visitInlinedFunctionBlock(inlinedBlock)
    }

    /**
     * Produces a call to the synthetic accessor [accessorSymbol] to replace the field _read_ expression [oldExpression].
     *
     * Before:
     * ```kotlin
     * class C {
     *     protected /*field*/ val myField: Int
     *
     *     internal inline fun foo(): Int = myField + 1
     * }
     * ```
     *
     * After:
     * ```kotlin
     * class C {
     *     protected /*field*/ val myField: Int
     *
     *     public static fun access$getMyField$p($this: C): Int =
     *         $this.myField
     *
     *     internal inline fun foo(): Int =
     *         C.access$getMyField$p(this) + 1
     * }
     * ```
     */
    private fun modifyGetterExpression(
        oldExpression: IrGetField,
        accessorSymbol: IrSimpleFunctionSymbol
    ): IrCall {
        val call = IrCallImpl(
            oldExpression.startOffset, oldExpression.endOffset,
            oldExpression.type,
            accessorSymbol, 0, accessorSymbol.owner.valueParameters.size,
            oldExpression.origin
        )
        oldExpression.receiver?.let {
            call.putValueArgument(0, oldExpression.receiver)
        }
        return call
    }

    /**
     * Produces a call to the synthetic accessor [accessorSymbol] to replace the field _write_ expression [oldExpression].
     *
     * Before:
     * ```kotlin
     * class C {
     *     protected var myField: Int = 0
     *
     *     internal inline fun foo(x: Int) {
     *         myField = x
     *     }
     * }
     * ```
     *
     * After:
     * ```kotlin
     * class C {
     *     protected var myField: Int = 0
     *
     *     public static fun access$setMyField$p($this: C, <set-?>: Int) {
     *         $this.myField = <set-?>
     *     }
     *
     *     internal inline fun foo(x: Int) {
     *         access$setMyField$p(this, x)
     *     }
     * }
     * ```
     */
    private fun modifySetterExpression(
        oldExpression: IrSetField,
        accessorSymbol: IrSimpleFunctionSymbol
    ): IrCall {
        val call = IrCallImpl(
            oldExpression.startOffset, oldExpression.endOffset,
            oldExpression.type,
            accessorSymbol, 0, accessorSymbol.owner.valueParameters.size,
            oldExpression.origin
        )
        oldExpression.receiver?.let {
            call.putValueArgument(0, oldExpression.receiver)
        }
        call.putValueArgument(call.valueArgumentsCount - 1, oldExpression.value)
        return call
    }
}

private fun IrField.resolveFieldFakeOverride(): IrField {
    val correspondingProperty = correspondingPropertySymbol?.owner
    if (correspondingProperty == null || !correspondingProperty.isFakeOverride)
        return this
    return correspondingProperty.resolveFakeOverrideOrFail().backingField
        ?: throw AssertionError(
            "Fake override property ${correspondingProperty.render()} with backing field " +
                    "overrides a real property with no backing field: ${correspondingProperty.resolveFakeOverrideOrFail().render()}"
        )
}
