/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.lower.LocalClassPopupLowering
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.findInlineLambdas
import org.jetbrains.kotlin.ir.declarations.*

@PhaseDescription(
    name = "JvmLocalClassExtraction",
    description = "Move local classes from field initializers and anonymous init blocks into the containing class"
)
internal class JvmLocalClassPopupLowering(context: JvmBackendContext) : LocalClassPopupLowering(context) {
    private val inlineLambdaToScope = mutableMapOf<IrFunction, IrDeclaration>()

    override fun lower(irFile: IrFile) {
        irFile.findInlineLambdas(context as JvmBackendContext) { argument, _, _, scope ->
            inlineLambdaToScope[argument.symbol.owner] = scope
        }
        super.lower(irFile)
        inlineLambdaToScope.clear()
    }

    // On JVM, we only pop up local classes in field initializers and anonymous init blocks, so that InitializersLowering would not copy
    // them to each constructor. (Moving all local classes is not possible because of cases where they use reified type parameters,
    // or capture crossinline lambdas.)
    // Upon moving such class, we record that it used to be in an initializer so that the codegen later sets its EnclosingMethod
    // to the primary constructor.
    override fun shouldPopUp(klass: IrClass, currentScope: ScopeWithIr?): Boolean {
        // On JVM, lambdas have package-private visibility after LocalDeclarationsLowering; see `forClass` in `localDeclarationsPhase`.
        if (!super.shouldPopUp(klass, currentScope) && !klass.isGeneratedLambdaClass) return false

        var parent = currentScope?.irElement
        while (parent is IrFunction) {
            parent = inlineLambdaToScope[parent] ?: break
        }

        if (parent is IrAnonymousInitializer && !parent.isStatic ||
            parent is IrField && !parent.isStatic
        ) {
            (context as JvmBackendContext).isEnclosedInConstructor.add(klass.attributeOwnerId)
            return true
        }
        return false
    }
}
