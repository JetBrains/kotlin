/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat


class RemoveInlineFunctionsWithReifiedTypeParametersLowering: DeclarationContainerLoweringPass {
    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.transformDeclarationsFlat {
            if (it is IrFunction && it.isInline && it.typeParameters.any { it.isReified }) listOf() else null
        }
    }
}
