/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.lower.LocalClassPopupLowering
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.IrInlineReferenceLocator
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class JvmLocalClassPopupLowering(context: JvmBackendContext) : LocalClassPopupLowering(context) {
    private val inlineLambdaToScope = mutableMapOf<IrFunction, IrDeclaration>()

    override fun lower(irFile: IrFile) {
        irFile.accept(object : IrInlineReferenceLocator(context as JvmBackendContext) {
            override fun visitInlineLambda(
                argument: IrFunctionReference, callee: IrFunction, parameter: IrValueParameter, scope: IrDeclaration
            ) {
                inlineLambdaToScope[argument.symbol.owner] = scope
            }
        }, null)
        super.lower(irFile)
        inlineLambdaToScope.clear()
    }

    // On JVM, we only pop up local classes in field initializers and anonymous init blocks, so that InitializersLowering would not copy
    // them to each constructor. (Moving all local classes is not possible because of cases where they use reified type parameters,
    // or capture crossinline lambdas.)
    // Upon moving such class, we record customEnclosingFunction for it to be the class constructor. This is needed because otherwise
    // the class will not get any EnclosingMethod in the codegen later, since it won't be local anymore.
    override fun shouldPopUp(klass: IrClass, currentScope: ScopeWithIr?): Boolean {
        // On JVM, lambdas have package-private visibility after LocalDeclarationsLowering, so we have to check something else.
        val isLocal = super.shouldPopUp(klass, currentScope) ||
                klass.origin == JvmLoweredDeclarationOrigin.LAMBDA_IMPL ||
                klass.origin == JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL ||
                klass.origin == JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE
        if (!isLocal) return false

        var parent = currentScope?.irElement
        while (parent is IrFunction) {
            parent = inlineLambdaToScope[parent] ?: break
        }
        val container = when (parent) {
            is IrAnonymousInitializer -> parent.parentAsClass.takeUnless { parent.isStatic }
            is IrField -> parent.parentAsClass.takeUnless { parent.isStatic }
            else -> null
        } ?: return false

        // In case there's no primary constructor, it's unclear which constructor should be the enclosing one, so we select the first.
        (context as JvmBackendContext).customEnclosingFunction[klass.attributeOwnerId] =
            container.primaryConstructor ?: container.declarations.firstIsInstanceOrNull<IrConstructor>()
                    ?: error("Class in a non-static initializer found, but container has no constructors: ${container.render()}")

        return true
    }
}
