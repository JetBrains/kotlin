/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.*
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.builder.buildBlock
import org.jetbrains.kotlin.fir.generateTemporaryVariable
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

data class NameBasedDestructuringDeclaration(
    val entries: List<NameBasedDestructuringEntry>,
    val initializer: FirExpression,
    val source: KtSourceElement,
    // TODO: implement annotations
//    val annotations: List<FirAnnotation>,
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
        )
        return buildBlock {
            source = this@NameBasedDestructuringDeclaration.source
            with(builder) {
                addNameBasedDestructuringStatements(
                    statements,
                    moduleData,
                    this@NameBasedDestructuringDeclaration,
                    baseVariable,
                    tmpVariable,
                    forceLocal = false
                )
            }
        }
    }
}

class NameBasedDestructuringEntry(
    val isVar: Boolean,
    val source: KtSourceElement,
    val returnTypeRef: FirTypeRef,
    val name: Name,
    val propertyAccessor: Name,
    // TODO: implement annotations later
//    val annotations: List<FirAnnotationCall>,
) {
    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    companion object : NameBasedDestructuringContext<NameBasedDestructuringEntry> {
        override val NameBasedDestructuringEntry.returnTypeRef: FirTypeRef get() = returnTypeRef
        override val NameBasedDestructuringEntry.name: Name get() = name
        override val NameBasedDestructuringEntry.isVar: Boolean get() = isVar
        override val NameBasedDestructuringEntry.source: KtSourceElement get() = source
        override val NameBasedDestructuringEntry.propertyAccessorName: Name get() = propertyAccessor
    }
}

fun AbstractRawFirBuilder<*>.addNameBasedDestructuringStatements(
    destination: MutableList<FirStatement>,
    moduleData: FirModuleData,
    multiDeclaration: NameBasedDestructuringDeclaration,
    container: FirVariable,
    tmpVariable: Boolean,
    forceLocal: Boolean,

    ) {
    addNameBasedDestructuringVariables(
        destination,
        NameBasedDestructuringEntry,
        moduleData,
        container,
        multiDeclaration.entries,
        tmpVariable,
        forceLocal
    )
}
