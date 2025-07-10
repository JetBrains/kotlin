/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.types.typeContext

enum class PermissivenessWithMigration {
    LESS,
    SAME,
    MORE,
    UNKNOWN,
    UNKNOW_WITH_MIGRATION,
}


context(sessionHolder: SessionHolder)
fun EffectiveVisibility.relationWithMigration(other: EffectiveVisibility): PermissivenessWithMigration {
    if (this is EffectiveVisibility.InternalOrPackage
        && other is EffectiveVisibility.InternalOrPackage
        && this != other
        && !sessionHolder.session.languageVersionSettings.supportsFeature(LanguageFeature.ForbidExposingPackagePrivateInInternal)
    ) {
        return PermissivenessWithMigration.UNKNOW_WITH_MIGRATION
    }

    return when (relation(other, sessionHolder.session.typeContext)) {
        EffectiveVisibility.Permissiveness.LESS -> PermissivenessWithMigration.LESS
        EffectiveVisibility.Permissiveness.SAME -> PermissivenessWithMigration.SAME
        EffectiveVisibility.Permissiveness.MORE -> PermissivenessWithMigration.MORE
        EffectiveVisibility.Permissiveness.UNKNOWN -> PermissivenessWithMigration.UNKNOWN
    }
}