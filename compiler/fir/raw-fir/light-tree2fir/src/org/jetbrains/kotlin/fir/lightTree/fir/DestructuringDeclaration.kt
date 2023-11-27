/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilder
import org.jetbrains.kotlin.fir.builder.DestructuringContext
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.addDestructuringVariables
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

data class DestructuringDeclaration(
    val isVar: Boolean,
    val entries: List<DestructuringEntry>,
    val initializer: FirExpression,
    val source: KtSourceElement,
    val annotations: List<FirAnnotation>,
) {
    context(AbstractRawFirBuilder<*>)
    fun toFirDestructingDeclaration(
        moduleData: FirModuleData,
        tmpVariable: Boolean = true,
        localEntries: Boolean = true,
    ): FirExpression {
        val baseVariable = generateTemporaryVariable(
            moduleData,
            source,
            SpecialNames.DESTRUCT,
            initializer,
            extractedAnnotations = annotations
        )
        return buildBlock {
            statements.addDestructuringStatements(moduleData, this@DestructuringDeclaration, baseVariable, tmpVariable, localEntries)
        }
    }
}

class DestructuringEntry(
    val source: KtSourceElement,
    val returnTypeRef: FirTypeRef,
    val name: Name,
    val annotations: List<FirAnnotationCall>,
) {
    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    companion object : DestructuringContext<DestructuringEntry> {
        override val DestructuringEntry.returnTypeRef: FirTypeRef get() = returnTypeRef
        override val DestructuringEntry.name: Name get() = name
        override val DestructuringEntry.source: KtSourceElement get() = source
        override fun DestructuringEntry.extractAnnotationsTo(target: FirAnnotationContainerBuilder, containerSymbol: FirBasedSymbol<*>) {
            target.annotations += annotations.map {
                buildAnnotationCallCopy(it) {
                    containingDeclarationSymbol = containerSymbol
                }
            }
        }
    }
}

context(AbstractRawFirBuilder<*>)
fun MutableList<FirStatement>.addDestructuringStatements(
    moduleData: FirModuleData,
    multiDeclaration: DestructuringDeclaration,
    container: FirVariable,
    tmpVariable: Boolean,
    localEntries: Boolean,
) {
    with(DestructuringEntry) {
        addDestructuringVariables(
            moduleData,
            container,
            multiDeclaration.entries,
            multiDeclaration.isVar,
            tmpVariable,
            localEntries
        )
    }
}