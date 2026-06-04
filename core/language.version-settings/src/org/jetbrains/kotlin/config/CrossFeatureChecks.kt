/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

internal class CrossFeatureChecksResultsCollector {
    class FailedCheck(val message: String)

    val failedChecks: List<FailedCheck>
        field = mutableListOf()

    fun addFailedCheck(message: String) {
        failedChecks += FailedCheck(message)
    }
}

context(context: CrossFeatureChecksResultsCollector)
internal fun LanguageFeature.checkEnabledNotEarlierThan(vararg otherFeatures: LanguageFeature) {
    sinceVersion?.let {
        for (other in otherFeatures) {
            if (other.sinceVersion == null || other.sinceVersion > sinceVersion) {
                context.addFailedCheck("Expected $this.sinceVersion >= $other.sinceVersion")
            }
        }
    }
}

/**
 * @param sinceVersionMustBeSet if `true`, also check that the feature targets some version once dependee targets one
 */
context(context: CrossFeatureChecksResultsCollector)
internal fun LanguageFeature.checkEnabledLaterThan(
    vararg otherFeatures: LanguageFeature,
    sinceVersionMustBeSet: Boolean = false,
) {
    if (sinceVersion != null) {
        for (other in otherFeatures) {
            if (other.sinceVersion == null || other.sinceVersion >= sinceVersion) {
                context.addFailedCheck("Expected $this.sinceVersion > $other.sinceVersion")
            }
        }
    } else if (sinceVersionMustBeSet) {
        for (other in otherFeatures) {
            if (other.sinceVersion != null) {
                context.addFailedCheck("Expected $this.sinceVersion != null because $other.sinceVersion != null")
            }
        }
    }
}
