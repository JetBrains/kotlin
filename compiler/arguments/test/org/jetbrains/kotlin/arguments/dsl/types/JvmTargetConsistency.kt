/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import org.jetbrains.kotlin.arguments.JvmTarget
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class JvmTargetConsistency {
    @Test
    fun supportedVersionsDescriptionIsConsistent() {
        val lastSupportedTarget = org.jetbrains.kotlin.config.JvmTarget.entries.last()
        assertTrue("Please update the `JvmTarget.SUPPORTED_VERSIONS_DESCRIPTION`") {
            JvmTarget.SUPPORTED_VERSIONS_DESCRIPTION.endsWith(lastSupportedTarget.description)
        }
    }
}
