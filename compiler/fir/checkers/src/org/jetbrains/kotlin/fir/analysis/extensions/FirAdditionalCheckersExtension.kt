/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.extensions.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class FirAdditionalCheckersExtension(session: FirSession) : AbstractFirAdditionalCheckersExtension(session) {
    companion object {
        val NAME = FirExtensionPointName("ExtensionCheckers")
    }

    open val declarationCheckers: DeclarationCheckers = DeclarationCheckers.EMPTY
    open val expressionCheckers: ExpressionCheckers = ExpressionCheckers.EMPTY

    final override val name: FirExtensionPointName
        get() = NAME

    final override val directlyApplicableAnnotations: Set<AnnotationFqn>
        get() = emptySet()

    final override val childrenApplicableAnnotations: Set<AnnotationFqn>
        get() = emptySet()

    final override val metaAnnotations: Map<AnnotationFqn, MetaAnnotationMode>
        get() = emptyMap()

    final override val mode: Mode
        get() = Mode.ALL
}

val FirOldExtensionsService.additionalCheckers: List<FirAdditionalCheckersExtension> by FirOldExtensionsService.additionalCheckers()

private fun FirOldExtensionsService.Companion.additionalCheckers(): ReadOnlyProperty<FirOldExtensionsService, List<FirAdditionalCheckersExtension>> {
    val accessor = generateAccessor<FirRegisteredExtension<AbstractFirAdditionalCheckersExtension>, AbstractFirAdditionalCheckersExtension>(
        AbstractFirAdditionalCheckersExtension::class
    )
    return object : ReadOnlyProperty<FirOldExtensionsService, List<FirAdditionalCheckersExtension>> {
        override fun getValue(thisRef: FirOldExtensionsService, property: KProperty<*>): List<FirAdditionalCheckersExtension> {
            val extensions = accessor.getValue(thisRef, property)
            @Suppress("UNCHECKED_CAST")
            return extensions.extensionsWithAllMode as List<FirAdditionalCheckersExtension>
        }
    }
}