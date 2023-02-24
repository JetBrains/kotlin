/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.types.extractTypeParameters
import org.jetbrains.kotlin.ir.util.*

/**
 * Lifts local classes from its containing function to the closest parent [IrClass] or [IrScript].
 *
 * Also, rewrites the extracted class's type parameters in the following way (pseudocode):
 *
 * ```kotlin
 * class Outer<OuterTP>(val a: OuterTP) {
 *     fun foo<FooTP>(b: FooTP) {
 *         /*local*/ class Local<LocalTP>(
 *             val p0: LocalTP,
 *             val p1: FooTP,
 *             val p2: OuterTP
 *         )
 *
 *         println(Local<Int>(42, b, a))
 *     }
 * }
 * ```
 *
 * Is transformed into:
 *
 * ```kotlin
 * class Outer<OuterTP>(val a: OuterTP) {
 *
 *     /*local*/ class Local<LocalTP, SynthesizedFooTP>(
 *         val p0: LocalTP,
 *         val p1: SynthesizedFooTP,
 *         val p2: OuterTP
 *     )
 *
 *     fun foo<FooTP>(b: FooTP) {
 *         println(Local<Int, FooTP>(42, b, a))
 *     }
 * }
 * ```
 */
open class LocalClassPopupLowering(
    val context: BackendContext,
    val recordExtractedLocalClasses: BackendContext.(IrClass) -> Unit = {},
) : BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile, withLocalDeclarations = true)
    }

    private data class ExtractedLocalClass(
        val local: IrClass,
        val newContainer: IrDeclarationParent,
        val extractedUnder: IrStatement?,
        val typeParameterRemapper: IrTypeParameterRemapper,
    )

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val extractedLocalClasses = collectLocalClasses(irBody, container)

        liftLocalClasses(extractedLocalClasses)
    }

    private fun collectLocalClasses(
        irBody: IrBody,
        container: IrDeclaration
    ): List<ExtractedLocalClass> {
        val extractedLocalClasses = arrayListOf<ExtractedLocalClass>()

        irBody.transform(object : IrElementTransformerVoidWithContext() {

            override fun visitClassNew(declaration: IrClass): IrStatement {
                val currentScope =
                    if (allScopes.size > 1) allScopes[allScopes.lastIndex - 1] else createScope(container as IrSymbolOwner)
                if (!shouldPopUp(declaration, currentScope)) return declaration

                val currentTypeParameters = extractTypeParameters(declaration)

                var extractedUnder: IrStatement? = declaration
                var newContainer = declaration.parent
                while (newContainer is IrDeclaration && newContainer !is IrClass && newContainer !is IrScript) {
                    extractedUnder = newContainer
                    newContainer = newContainer.parent
                }

                val newContainerTypeParameters = extractTypeParameters(newContainer)
                val typeParametersToKeep = currentTypeParameters - newContainerTypeParameters.toSet()

                declaration.typeParameters = emptyList()
                val newTypeParameters = declaration.copyTypeParameters(typeParametersToKeep)
                val typeParameterRemapper = IrTypeParameterRemapper(currentTypeParameters.zip(newTypeParameters).toMap())

                when (newContainer) {
                    is IrStatementContainer -> {
                        // TODO: check if it is the correct behavior
                        if (extractedUnder == declaration) {
                            extractedUnder = (newContainer.statements.indexOf(extractedUnder) + 1)
                                .takeIf { it > 0 && it < newContainer.statements.size }
                                ?.let { newContainer.statements[it] }
                        }
                        extractedLocalClasses.add(ExtractedLocalClass(declaration, newContainer, extractedUnder, typeParameterRemapper))
                    }
                    is IrDeclarationContainer -> extractedLocalClasses.add(
                        ExtractedLocalClass(
                            declaration,
                            newContainer,
                            extractedUnder,
                            typeParameterRemapper
                        )
                    )
                    else -> compilationException("Unexpected container type ${newContainer.render()}", declaration)
                }

                return IrCompositeImpl(declaration.startOffset, declaration.endOffset, context.irBuiltIns.unitType)
            }
        }, null)

        return extractedLocalClasses
    }

    private fun liftLocalClasses(extractedLocalClasses: List<ExtractedLocalClass>) {
        for ((local, newContainer, extractedUnder, typeParameterRemapper) in extractedLocalClasses) {
            when (newContainer) {
                is IrStatementContainer -> {
                    val insertIndex = extractedUnder?.let { newContainer.statements.indexOf(it) } ?: -1
                    if (insertIndex >= 0) {
                        newContainer.statements.add(insertIndex, local)
                    } else {
                        newContainer.statements.add(local)
                    }
                    local.setDeclarationsParent(newContainer)
                }
                is IrDeclarationContainer -> {
                    newContainer.addChild(local)
                }
                else -> compilationException("Unexpected container type $newContainer", local)
            }
            local.remapTypes(typeParameterRemapper)
            context.recordExtractedLocalClasses(local)
        }
    }

    protected open fun shouldPopUp(klass: IrClass, currentScope: ScopeWithIr?): Boolean =
        klass.isLocalNotInner()
}
