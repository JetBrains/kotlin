/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.migration

import org.jetbrains.kotlin.idea.configuration.MigrationInfo

/**
 * Marker interface for inspections that can be used during kotlin migrations
 */
interface MigrationFix {
    fun isApplicable(migrationInfo: MigrationInfo): Boolean
}
