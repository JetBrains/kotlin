/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.psiUtil.pureEndOffset
import org.jetbrains.kotlin.psi.psiUtil.pureStartOffset
import org.jetbrains.kotlin.resolve.BindingContext

class ScriptGenerator(declarationGenerator: DeclarationGenerator) : DeclarationGeneratorExtension(declarationGenerator) {
    fun generateScriptDeclaration(ktScript: KtScript): IrDeclaration? {
        val descriptor = getOrFail(BindingContext.DECLARATION_TO_DESCRIPTOR, ktScript) as ScriptDescriptor

        return context.symbolTable.declareScript(descriptor).buildWithScope { irScript ->
            val startOffset = ktScript.pureStartOffset
            val endOffset = ktScript.pureEndOffset

            irScript.thisReceiver = context.symbolTable.declareValueParameter(
                startOffset, endOffset,
                IrDeclarationOrigin.INSTANCE_RECEIVER,
                descriptor.thisAsReceiverParameter,
                descriptor.thisAsReceiverParameter.type.toIrType()
            ).also { it.parent = irScript }

            for (d in ktScript.declarations) {
                if (d is KtScriptInitializer) irScript.statements += BodyGenerator(
                    irScript.symbol,
                    context
                ).generateExpressionBody(d.body!!).expression
                else irScript.declarations += declarationGenerator.generateMemberDeclaration(d)!!
            }
        }
    }
}
