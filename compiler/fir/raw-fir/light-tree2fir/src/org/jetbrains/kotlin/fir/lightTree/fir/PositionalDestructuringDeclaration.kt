/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.*
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCallCopy
import org.jetbrains.kotlin.fir.expressions.builder.buildBlock
import org.jetbrains.kotlin.fir.generateTemporaryVariable
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

data class PositionalDestructuringDeclaration(
    val isVar: Boolean,
    val entries: List<PositionalDestructuringEntry>,
    val initializer: FirExpression,
    val source: KtSourceElement,
    val annotations: List<FirAnnotation>,
) {
    fun toFirDeclaration(
        builder: AbstractRawFirBuilder<*>,
        moduleData: FirModuleData,
        tmpVariable: Boolean = true,
    ): FirExpression {
        val baseVariable = generateTemporaryVariable(
            moduleData,
            source,
            SpecialNames.DESTRUCT,
            initializer,
            extractedAnnotations = annotations
        )
        return buildBlock {
            source = this@PositionalDestructuringDeclaration.source
            with(builder) {
                addDestructuringStatements(
                    statements,
                    moduleData,
                    this@PositionalDestructuringDeclaration,
                    baseVariable,
                    tmpVariable,
                    forceLocal = false
                )
            }
        }
    }
}

class PositionalDestructuringEntry(
    val source: KtSourceElement,
    val returnTypeRef: FirTypeRef,
    val name: Name,
    val annotations: List<FirAnnotationCall>,
) {
    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    companion object : PositionalDestructuringContext<PositionalDestructuringEntry> {
        override val PositionalDestructuringEntry.returnTypeRef: FirTypeRef get() = returnTypeRef
        override val PositionalDestructuringEntry.name: Name get() = name
        override val PositionalDestructuringEntry.source: KtSourceElement get() = source
        override fun PositionalDestructuringEntry.extractAnnotationsTo(
            target: FirAnnotationContainerBuilder,
            containerSymbol: FirBasedSymbol<*>,
        ) {
            target.annotations += annotations.map {
                buildAnnotationCallCopy(it) {
                    containingDeclarationSymbol = containerSymbol
                }
            }
        }
    }
}

fun AbstractRawFirBuilder<*>.addDestructuringStatements(
    destination: MutableList<FirStatement>,
    moduleData: FirModuleData,
    multiDeclaration: PositionalDestructuringDeclaration,
    container: FirVariable,
    tmpVariable: Boolean,
    forceLocal: Boolean

) {
    addPositionalDestructuringVariables(
        destination,
        PositionalDestructuringEntry,
        moduleData,
        container,
        multiDeclaration.entries,
        multiDeclaration.isVar,
        tmpVariable,
        forceLocal
    )
}
