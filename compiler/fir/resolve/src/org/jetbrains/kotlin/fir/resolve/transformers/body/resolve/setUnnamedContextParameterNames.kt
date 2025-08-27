/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.declarationNameInvalidChars
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.generatedContextParameterName
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import kotlin.collections.iterator

context(sessionHolder: SessionHolder)
internal fun FirCallableDeclaration.setUnnamedContextParameterNames() {
    if (contextParameters.isEmpty()) return

    val contextParameterNames = contextParameters
        .filter { it.name.isSpecial && !it.returnTypeRef.coneType.hasError() }
        .ifEmpty { return }
        .associateWith {
            it.returnTypeRef.coneType
                .erasedUpperBoundName(tryApproximation = true)
                ?.toString()
                ?.replaceInvalidChars(sessionHolder.session.declarationNameInvalidChars)
                ?: errorWithAttachment("Cannot compute generated name for context parameter") {
                    withFirEntry("contextParameter", it)
                    withFirEntry("containingDeclaration", this@setUnnamedContextParameterNames)
                }
        }

    val nameGroups = contextParameterNames.entries.groupBy({ it.value }, { it.key })

    for ((contextParameter, baseName) in contextParameterNames) {
        val currentNameGroup = nameGroups[baseName]!!
        val suffix = if (currentNameGroup.size == 1) "" else "#" + (currentNameGroup.indexOf(contextParameter) + 1)
        contextParameter.generatedContextParameterName = Name.identifier($$"$context-$$baseName$$suffix")
    }
}

context(sessionHolder: SessionHolder)
private fun ConeKotlinType.erasedUpperBoundName(tryApproximation: Boolean): Name? {
    return when (this) {
        is ConeTypeParameterType -> {
            val bounds = lookupTag.symbol.resolvedBounds
            for (bound in bounds) {
                val type = bound.coneType.fullyExpandedType()
                val classSymbol = type.toClassSymbol() ?: continue
                if (classSymbol.classKind != ClassKind.ANNOTATION_CLASS && classSymbol.classKind != ClassKind.INTERFACE) {
                    return classSymbol.name
                }
            }
            bounds.first().coneType.erasedUpperBoundName(tryApproximation)
        }
        is ConeLookupTagBasedType -> fullyExpandedType().lookupTagIfAny?.name
        is ConeDefinitelyNotNullType -> original.erasedUpperBoundName(tryApproximation)
        is ConeFlexibleType -> upperBound.erasedUpperBoundName(tryApproximation)
        else if tryApproximation -> {
            sessionHolder.session.typeApproximator
                .approximateToSuperType(this, TypeApproximatorConfiguration.FrontendToBackendTypesApproximation)
                ?.erasedUpperBoundName(tryApproximation = false)
        }
        else -> null
    }
}

private fun String.replaceInvalidChars(invalidChars: Set<Char>) =
    invalidChars.fold(this) { acc, ch -> if (ch in acc) acc.replace(ch, '_') else acc }