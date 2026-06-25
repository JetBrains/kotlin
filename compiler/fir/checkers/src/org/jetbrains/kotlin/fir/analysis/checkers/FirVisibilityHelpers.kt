/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.getPrimaryConstructorSymbol
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.name.StandardClassIds

enum class PermissivenessForExposedVisibility {
    LESS,
    SAME,
    MORE,
    UNKNOWN,

    /**
     * Special value for relation between internal and package private.
     * Before its introduction, it was treated as [SAME].
     *
     * Because exposing package private types from internal declarations can lead to runtime crashes,
     * it was planned to deprecate it.
     * However, because there is no real workaround for users, it was decided to postpone the deprecation indefinitely.
     * See KTLC-271.
     *
     * Currently, this value will not lead to a diagnostic, unless [LanguageFeature.ForbidExposingPackagePrivateInInternal] is enabled,
     * in which case a warning will be reported.
     */
    PACKAGE_PRIVATE_FROM_INTERNAL,
}


context(sessionHolder: SessionHolder)
fun EffectiveVisibility.relationForExposedVisibility(other: EffectiveVisibility): PermissivenessForExposedVisibility {
    if (this is EffectiveVisibility.InternalOrPackage
        && other is EffectiveVisibility.InternalOrPackage
        && this != other
    ) {
        return PermissivenessForExposedVisibility.PACKAGE_PRIVATE_FROM_INTERNAL
    }

    return when (relation(other, sessionHolder.session.typeContext)) {
        EffectiveVisibility.Permissiveness.LESS -> PermissivenessForExposedVisibility.LESS
        EffectiveVisibility.Permissiveness.SAME -> PermissivenessForExposedVisibility.SAME
        EffectiveVisibility.Permissiveness.MORE -> PermissivenessForExposedVisibility.MORE
        EffectiveVisibility.Permissiveness.UNKNOWN -> PermissivenessForExposedVisibility.UNKNOWN
    }
}

@OptIn(SymbolInternals::class)
context(context: CheckerContext)
fun FirBasedSymbol<*>.isExportedToJs(): Boolean {
    val declaration = fir
    val session = context.session

    if (declaration is FirMemberDeclaration) {
        val visibility = declaration.visibility
        if (visibility != Visibilities.Public && visibility != Visibilities.Protected) {
            return false
        }

        /**
         * We don't need to check anything except the `copy` method because
         * - `componentN` are not exported to JS at all. See [org.jetbrains.kotlin.ir.backend.js.lower.ExcludeSyntheticDeclarationsFromExportLowering]
         * - `toString`, `hashCode`, and `equals` are members of `Any` so they are exported anyway. See [org.jetbrains.kotlin.ir.backend.js.ir.shouldDeclarationBeExported]
         */
        if (
            declaration.origin == FirDeclarationOrigin.Synthetic.DataClassMember &&
            declaration.nameOrSpecialName != StandardNames.DATA_CLASS_COPY
        ) {
            return false
        }
    }

    if (hasAnnotationOrInsideAnnotatedClass(StandardClassIds.Annotations.jsExportIgnore, session)) {
        return false
    }

    if (
        hasAnnotationOrInsideAnnotatedClass(StandardClassIds.Annotations.jsExport, session) ||
        hasAnnotationOrInsideAnnotatedClass(StandardClassIds.Annotations.jsExportDefault, session) ||
        getAnnotationBooleanParameter(StandardClassIds.Annotations.jsImplicitExport, session) == true ||
        getContainingFile()?.symbol?.hasAnnotation(StandardClassIds.Annotations.jsExport, session) == true
    ) {
        /**
         * The rules for exporting data class copy functions are inheriting rules for consistent `copy` visibility,
         * described in KT-11914. The migration process is exactly the same as for the visibility modifiers (described in details in the same ticket)
         *
         * For more context see [shouldExportDataClassCopy] from org.jetbrains.kotlin.ir.backend.js.ir package
         */
        if (declaration is FirFunction && declaration.isCopyMethod()) {
            val dataClass = getContainingClassSymbol()
            val primaryConstructor = dataClass?.getPrimaryConstructorSymbol(session, context.scopeSession) ?: return false

            return when {
                dataClass.hasAnnotation(StandardClassIds.Annotations.ExposedCopyVisibility, session) -> true
                primaryConstructor.isExportedToJs() -> true
                LanguageFeature.DataClassCopyRespectsConstructorVisibility.isEnabled() -> false
                else -> !dataClass.hasAnnotation(StandardClassIds.Annotations.ConsistentCopyVisibility, session)
            }
        }

        return true
    }

    return false
}

fun FirBasedSymbol<*>.getContainingFile(): FirFile? {
    return when (this) {
        is FirCallableSymbol<*> -> moduleData.session.firProvider.getFirCallableContainerFile(this)
        is FirClassLikeSymbol<*> -> moduleData.session.firProvider.getFirClassifierContainerFileIfAny(this)
        else -> null
    }
}

