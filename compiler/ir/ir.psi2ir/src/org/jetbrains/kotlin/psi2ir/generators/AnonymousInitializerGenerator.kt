/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments

internal class AnonymousInitializerGenerator(
    declarationGenerator: DeclarationGenerator
) : DeclarationGeneratorExtension(declarationGenerator) {

    fun generateAnonymousInitializerDeclaration(
        ktAnonymousInitializer: KtAnonymousInitializer,
        irClass: IrClass
    ): IrAnonymousInitializer =
        context.symbolTable.descriptorExtension.declareAnonymousInitializer(
            ktAnonymousInitializer.startOffsetSkippingComments, ktAnonymousInitializer.endOffset,
            IrDeclarationOrigin.DEFINED, irClass.descriptor
        ).buildWithScope { irAnonymousInitializer ->
            irAnonymousInitializer.parent = irClass
            val bodyGenerator = createBodyGenerator(irAnonymousInitializer.symbol)
            val statementGenerator = bodyGenerator.createStatementGenerator()
            val ktBody = ktAnonymousInitializer.body!!
            val irBlockBody = context.irFactory.createBlockBody(ktBody.startOffsetSkippingComments, ktBody.endOffset)
            if (context.configuration.generateBodies) {
                if (ktBody is KtBlockExpression) {
                    statementGenerator.generateStatements(ktBody.statements, irBlockBody)
                } else {
                    irBlockBody.statements.add(statementGenerator.generateStatement(ktBody))
                }
            }
            irAnonymousInitializer.body = irBlockBody
        }
}
