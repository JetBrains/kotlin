/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationPopupLowering
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.findInlineLambdas
import org.jetbrains.kotlin.backend.jvm.ir.findRichInlineLambdas
import org.jetbrains.kotlin.backend.jvm.isEnclosedInConstructor
import org.jetbrains.kotlin.ir.declarations.*

/**
 * Moves local classes from field initializers and anonymous init blocks into the containing class.
 */
internal class JvmLocalDeclarationPopupLowering(context: JvmBackendContext) : LocalDeclarationPopupLowering(context) {
    private val inlineLambdaToScope = mutableMapOf<IrFunction, IrDeclaration>()

    override fun lower(irFile: IrFile) {
        // TODO remove after KT-78719
        irFile.findInlineLambdas(context as JvmBackendContext) { argument, _, _, scope ->
            inlineLambdaToScope[argument.symbol.owner] = scope
        }
        irFile.findRichInlineLambdas(context as JvmBackendContext) { argument, _, _, scope ->
            inlineLambdaToScope[argument.invokeFunction] = scope
        }
        super.lower(irFile)
        inlineLambdaToScope.clear()
    }

    // On JVM, we only pop up local classes in field initializers and anonymous init blocks, so that InitializersLowering would not copy
    // them to each constructor. (Moving all local classes is not possible because of cases where they use reified type parameters,
    // or capture crossinline lambdas.)
    // Upon moving such class, we record that it used to be in an initializer so that the codegen later sets its EnclosingMethod
    // to the primary constructor.
    override fun shouldPopUp(declaration: IrDeclaration, currentScope: ScopeWithIr?): Boolean {
        if (declaration is IrClass) {
            // On JVM, lambdas have package-private visibility after LocalDeclarationsLowering; see `forClass` in `localDeclarationsPhase`.
            if (!super.shouldPopUp(declaration, currentScope) && !declaration.isGeneratedLambdaClass) return false

            var parent = currentScope?.irElement
            while (parent is IrFunction) {
                parent = inlineLambdaToScope[parent] ?: break
            }

            if (parent is IrAnonymousInitializer && !parent.isStatic ||
                parent is IrField && !parent.isStatic
            ) {
                declaration.isEnclosedInConstructor = true
                return true
            }
            return false
        } else
            return super.shouldPopUp(declaration, currentScope)
    }
}
