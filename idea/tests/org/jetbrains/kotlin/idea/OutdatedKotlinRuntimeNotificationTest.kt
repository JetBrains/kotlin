/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea

import junit.framework.TestCase
import org.jetbrains.kotlin.idea.versions.isRuntimeOutdated
import org.junit.Assert


class KotlinRuntimeLibraryUtilTest : TestCase() {

    fun testOutdatedRuntime() {
        outdated("1.0.0-beta1-001", "0.12.15")
        outdated("1.0.0-beta1-001", "0.152.16")

        outdated("1.0.0-beta-1001", "0.12.15")
        outdated("1.0.0-beta-1000", "0.152.16")

        notOutdated("1.0.0-beta1-001", "1.0.0-beta1-001")

        notOutdated("1.0.0", "1.0.0")
        notOutdated("1.0.1", "1.0.1")

        outdated("1.0.0-beta1-002", "1.0.0-beta1-001")
        outdated("1.0.0-beta1-010", "1.0.0-beta1-009")
        outdated("1.0.0-beta1-100", "1.0.0-beta1-099")
        outdated("1.0.0-beta2-000", "1.0.0-beta1-999")
        outdated("1.1.0-beta1-000", "1.0.9-beta9-999")
        outdated("2.0.0-beta1-000", "1.9.9-beta9-999")

        outdated("1.0.0-beta-1001", "1.0.0-beta-1000")
        outdated("1.0.0-beta-1010", "1.0.0-beta-1009")
        outdated("1.0.0-beta-1100", "1.0.0-beta-1099")
        outdated("1.0.0-beta-2000", "1.0.0-beta-1999")
        outdated("1.0.1-beta-1001", "1.0.0-beta-9999")
        outdated("1.1.0-beta-1001", "1.0.9-beta-9999")
        outdated("2.0.0-beta-1001", "1.9.9-beta-9999")
    }

    private fun outdated(bundledRuntime: String, library: String) {
        Assert.assertTrue("Should be outdated: bundled=$bundledRuntime, library=$library",
                          isRuntimeOutdated(library, bundledRuntime))
    }

    private fun notOutdated(bundledRuntime: String, library: String) {
        Assert.assertFalse("Should NOT be outdated: bundled=$bundledRuntime, library=$library",
                           isRuntimeOutdated(library, bundledRuntime))
    }

}