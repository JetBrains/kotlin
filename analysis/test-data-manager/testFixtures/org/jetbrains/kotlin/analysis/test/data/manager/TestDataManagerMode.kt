/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import org.jetbrains.annotations.TestOnly

/**
 * Mode for the test data manager.
 *
 * This enum is duplicated from the convention plugin to avoid dependencies.
 */
enum class TestDataManagerMode {
    CHECK,
    UPDATE,
    ;

    companion object {
        val currentMode: TestDataManagerMode
            get() = currentModeOrNull ?: CHECK

        internal val currentModeOrNull: TestDataManagerMode?
            get() = when (System.getProperty(TEST_DATA_MANAGER_OPTIONS_MODE)?.lowercase()) {
                "check" -> CHECK
                "update" -> UPDATE
                else -> null
            }

        val isUnderTeamCity: Boolean
            get() = isUnderTeamCityOverride ?: (System.getenv("TEAMCITY_VERSION") != null)

        @get:TestOnly
        @set:TestOnly
        internal var isUnderTeamCityOverride: Boolean?
            get() = isUnderTeamCityOverrideThreadLocal.get()
            set(value) = isUnderTeamCityOverrideThreadLocal.set(value)

        private val isUnderTeamCityOverrideThreadLocal = ThreadLocal<Boolean?>()

        val isUnderIde: Boolean
            get() = System.getProperty("idea.active") != null
    }
}
