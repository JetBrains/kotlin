/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.ir.isPure
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.hasPureInitialization
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.isLeftoverAfterObjectPurification
import org.jetbrains.kotlin.ir.backend.js.objectInstanceField
import org.jetbrains.kotlin.ir.backend.js.staticInitFunction
import org.jetbrains.kotlin.ir.backend.js.utils.isObjectInstanceField
import org.jetbrains.kotlin.ir.backend.js.utils.isObjectInstanceGetter
import org.jetbrains.kotlin.ir.backend.js.utils.primaryConstructorReplacement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull

/**
 * Optimization: make object instance getter functions pure whenever it's possible.
 *
 * For regular `object` declarations, consider such a pre-optimized generated code:
 *
 * ```javascript
 * var Foo_instance;
 * function Foo() {
 *   this.x = 42;
 *   Foo_instance = this;
 * }
 * function Foo_getInstance() {
 *   if (Foo_instance == null) new Foo();
 *   return Foo_instance;
 * }
 * ```
 *
 * This lowering tries to modify all three parties: instance field, constructor and instance getter in a way that allows initializing the
 * object eagerly, instead of the lazy way.
 *
 * Such modifications are applied when applicable:
 * - Instance getter: conditional constructor invocation is removed, just return the instance field.
 * - Constructor: instance field initialization logic `Foo_instance = this;` is removed
 * - Instance field: now has initializer in-place, using the constructor directly: `var Foo_instance = new Foo()`
 *
 * Which results in such a processed code:
 * ```javascript
 * var Foo_instance = new Foo();
 * function Foo() {
 *   this.x = 42;
 * }
 * function Foo_getInstance() {
 *   return Foo_instance;
 * }
 * ```
 *
 * The optimization can be applied only if the object is considered "pure".
 * Currently, the effect checker for regular objects only checks the constructor:
 * - It has no superclass (only extends `Any`)
 * - Every statement in its constructor is pure:
 *     - the delegating `super()` call goes to `Any`
 *     - expressions are effect-free
 *     - field reads/writes only touch this object's own fields
 *     - the `instance = this` self-assignment is ignored as an effect
 *
 * More details of pure checking logic can be found at [isPureStatementForObjectInitialization] and at [isPure].
 *
 * For companion objects, the `static_init` static initializer comes into play.
 *
 * We check `static_init` for incompatible expressions:
 * - Super `static_init` calls are not compatible
 * - Static initializers coming from `companion` blocks are not compatible
 *
 * In case `companion object`'s `static_init` is compatible with purification, it's declaration is marked
 * using `isLeftoverAfterObjectPurification` attribute and later, depending on the backend:
 * - in JS, specialized [JsCleanupPurifiedLeftoverDeclarationsLowering] runs a dumb declaration removal phase. DCE has been already run
 *   at that moment, so we can't rely on it
 * - in Wasm, the declaration is removed by the regular DCE
 *
 * Leftover usages of such `static_init` for both backends are removed by [CleanupPurifiedLeftoverUsagesLowering].
 *
 * For both `object`s and `companion object`s, [InlineObjectsWithPureInitializationLowering] as the next step will replace
 * `_getInstance` call with `_instance` field access when applicable.
 */
open class PurifyObjectInstanceGettersLowering(val context: JsCommonBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        when (declaration) {
            is IrFunction if declaration.isObjectConstructor() -> declaration.removeInstanceFieldInitializationIfPossible()
            is IrSimpleFunction if declaration.isObjectInstanceGetter() -> declaration.purifyObjectGetterIfPossible()
            is IrClass if declaration.isCompanion -> declaration.staticInitFunction?.markStaticInitForRemovalIfPossible(declaration)
            is IrField if declaration.isObjectInstanceField() -> declaration.purifyObjectInstanceFieldIfPossible()
        }

        return null
    }

    private fun IrFunction.removeInstanceFieldInitializationIfPossible() {
        if (!parentAsClass.isPureObject()) return

        (body as? IrBlockBody)?.statements?.removeIf {
            it is IrSetField && it.symbol.owner.isObjectInstanceField()
        }
    }

    private fun IrSimpleFunction.purifyObjectGetterIfPossible() {
        val objectToCreate = returnType.classOrNull?.owner ?: return
        if (!objectToCreate.isPureObject()) return

        val body = (body as? IrBlockBody) ?: return
        val instanceField = objectToCreate.objectInstanceField ?: irError("Expect the object instance field to be created") {
            withIrEntry("objectToCreate", objectToCreate)
            withIrEntry("this", this@purifyObjectGetterIfPossible)
        }

        body.statements.clear()
        body.statements += JsIrBuilder.buildReturn(
            symbol,
            JsIrBuilder.buildGetField(instanceField.symbol),
            context.irBuiltIns.nothingType
        )
    }

    private fun IrSimpleFunction.markStaticInitForRemovalIfPossible(companionObject: IrClass) {
        if (!companionObject.isPureObject()) return

        isLeftoverAfterObjectPurification = true
    }

    private fun IrField.purifyObjectInstanceFieldIfPossible() {
        val objectToCreate = type.classOrNull?.owner ?: return
        if (!objectToCreate.isPureObject()) return

        val initializerExpression =
            objectToCreate.primaryConstructorReplacement?.let { JsIrBuilder.buildCall(it.symbol) }
                ?: objectToCreate.primaryConstructor?.let { JsIrBuilder.buildConstructorCall(it.symbol) }
                ?: irError("Object should contain a primary constructor") {
                    withIrEntry("objectToCreate", objectToCreate)
                    withIrEntry("this", this@purifyObjectInstanceFieldIfPossible)
                }

        initializer = context.irFactory.createExpressionBody(initializerExpression)
    }

    private fun IrDeclaration.isObjectConstructor() =
        (this is IrConstructor || isEs6ConstructorReplacement) && parentAsClass.isObject

    private fun IrClass.isPureObject() =
        this::hasPureInitialization.getOrSetIfNull {
            val constructor = primaryConstructorReplacement ?: primaryConstructor
            when {
                superClass != null -> false
                constructor?.body?.statements?.any { !it.isPureStatementForObjectInitialization(this@isPureObject) } == true ->
                    false
                isCompanion && staticInitFunction?.body?.isPureStaticInitFunctionBody() == false ->
                    false
                else -> true
            }
        }

    /**
     * As `static_init` has a synthetic well known structure, we assume that `static_init` function is pure.
     *
     * There are 2 cases when body of static_init can be impure:
     * - Static initializers from companion blocks are presented (`IrSetField` with the corresponding origin)
     * - Calls to super `static_init` functions are presented (`IrCall` with the corresponding origin)
     *
     * Otherwise, the `static_init` is safe and the corresponding companion object can be purified.
     */
    private fun IrBody.isPureStaticInitFunctionBody(): Boolean {
        var isPureStaticInitFunction = true
        // Using visitor here because static_init has 'try-catch' and 'if' structured nodes,
        // which means we no longer can rely on body statement list.
        acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                // Calls to `static_init` coming from super types are also not suitable in purification at the moment.
                if (expression.symbol.owner.origin == WebStaticInitializersDeclarationLowering.STATIC_CLASS_INITIALIZER) {
                    isPureStaticInitFunction = false
                    return
                }
                expression.acceptChildrenVoid(this)
            }

            override fun visitSetField(expression: IrSetField) {
                // Any `IrSetField` for a companion block-related initializer, which means we can't purify.
                if (expression.origin == WebStaticInitializersDeclarationLowering.STATIC_FIELD_INITIALIZER) {
                    isPureStaticInitFunction = false
                    return
                }
                expression.acceptChildrenVoid(this)
            }
        })

        return isPureStaticInitFunction
    }

    private fun IrStatement.isPureStatementForObjectInitialization(owner: IrClass): Boolean {
        return when (this) {
            is IrReturn if value.isPureStatementForObjectInitialization(owner) -> true
            // Only objects which don't have a class parent
            is IrDelegatingConstructorCall if symbol.owner.parent == context.irBuiltIns.anyClass.owner -> true
            is IrExpression if isPure(anyVariable = true, checkFields = false, symbols = context.symbols) -> true
            is IrContainerExpression if statements.all { it.isPureStatementForObjectInitialization(owner) } -> true
            is IrVariable if (isEs6DelegatingConstructorCallReplacement || initializer?.isPureStatementForObjectInitialization(owner) != false) -> true
            // Only fields of the objects are safe to not save an intermediate state of another class/object/global
            is IrGetField if receiver?.isPureStatementForObjectInitialization(owner) == true -> true
            is IrSetField if receiver?.isPureStatementForObjectInitialization(owner) == true && value.isPureStatementForObjectInitialization(owner) -> true
            // Only current object could be initialized inside the object constructor, so we need to ignore it as an effect
            is IrSetField if symbol.owner.isObjectInstanceField() -> true
            is IrSetValue if symbol.owner.isOriginallyLocal && value.isPureStatementForObjectInitialization(owner) -> true
            else -> false
        }
    }
}
