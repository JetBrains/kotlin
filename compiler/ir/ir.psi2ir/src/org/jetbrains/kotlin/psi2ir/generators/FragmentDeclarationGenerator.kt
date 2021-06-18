/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.codegen.CodeFragmentCodegenInfo
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.psi.KtBlockCodeFragment
import org.jetbrains.kotlin.types.KotlinType

class FragmentDeclarationGenerator(override val context: GeneratorContext) : Generator {
    private val typeTranslator = context.typeTranslator

    fun KotlinType.toIrType() = typeTranslator.translateType(this)

    fun generateClassForCodeFragment(ktFile: KtBlockCodeFragment, codegenInfo: CodeFragmentCodegenInfo): IrClass =
        FragmentClassGenerator(this).generateClassForCodeFragment(ktFile, codegenInfo)

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun generateScopedTypeParameterDeclarations(
        irTypeParametersOwner: IrTypeParametersContainer
    ) {
        for (irTypeParameter in irTypeParametersOwner.typeParameters) {
            irTypeParameter.superTypes = irTypeParameter.descriptor.upperBounds.map {
                it.toIrType()
            }
        }
    }

    fun generateFunctionForFragment(ktFile: KtBlockCodeFragment, codegenInfo: CodeFragmentCodegenInfo): IrSimpleFunction {
        return FragmentFunctionGenerator(this, codegenInfo).generateFunctionForFragment(ktFile)
    }
}

abstract class FragmentDeclarationGeneratorExtension(val declarationGenerator: FragmentDeclarationGenerator) : Generator {

    override val context: GeneratorContext get() = declarationGenerator.context

    inline fun <T : IrDeclaration> T.buildWithScope(builder: (T) -> Unit): T =
        also { irDeclaration ->
            context.symbolTable.withScope(irDeclaration) {
                builder(irDeclaration)
            }
        }

    fun KotlinType.toIrType() = with(declarationGenerator) { toIrType() }
}
