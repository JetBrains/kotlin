/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.migration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.configuration.MigrationInfo
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.quickfix.migration.MigrationFix

class DivergedInferenceMigrationInspection : AbstractKotlinInspection(), MigrationFix {
    override fun isApplicable(migrationInfo: MigrationInfo): Boolean {
        return migrationInfo.isInferenceUpdate()
    }


}

private fun MigrationInfo.isInferenceUpdate(): Boolean {
    return !oldLanguageVersion.supportsNewInference() && newLanguageVersion.supportsNewInference()
}

private fun LanguageVersion.supportsNewInference(): Boolean = this >= LanguageFeature.NewInference.sinceVersion!!
