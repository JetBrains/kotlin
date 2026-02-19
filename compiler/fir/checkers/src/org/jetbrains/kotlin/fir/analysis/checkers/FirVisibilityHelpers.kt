/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.types.typeContext

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