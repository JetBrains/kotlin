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
import org.jetbrains.kotlin.idea.versions.pluginRuntimeVersion
import org.junit.Assert


class KotlinRuntimeLibraryUtilTest : TestCase() {
    fun testKotlinLibraryRelevantVersion() {
        test("0.10.2013", "0.10.2013")
        test("0.10.M.2013", "0.10")
        test("0.10.2.Idea140.2013", "0.10.2")
        test("0.11.1995.1.M.Idea140.2013", "0.11.1995.1")
        test("Some.0.10.2", "Some.0.10.2")
        test("@snapshot@", "@snapshot@")
        test("snapshot", "snapshot")
        test("internal-0.1.2", "internal-0.1.2")
        test(".0.1.2", ".0.1.2")
        test("0.1.2.", "0.1.2.")

        test("1.0.0-beta1-001-Idea141-12", "1.0.0-beta1-001")
        test("1.0.1-beta5-013-Idea143-1", "1.0.1-beta5-013")
        test("1.0.1-beta1-2-Idea143-1", "1.0.1-beta1-2")
        test("1.0.3-beta1-2", "1.0.3-beta1-2")

        test("1.0.0-beta1-001-IJ143-12", "1.0.0-beta1-001")
        test("1.0.1-beta5-013-IJ142-1", "1.0.1-beta5-013")
        test("1.0.1-beta1-2-IJ-2-1", "1.0.1-beta1-2")
        test("1.0.3-beta1-2-IJ", "1.0.3-beta1-2")

        test("1.0.3-beta-1020", "1.0.3-beta-1020")
        test("1.2.3-beta-2059-Idea141-1", "1.2.3-beta-2059")
        test("1.0.3-beta-4000-IJ-1", "1.0.3-beta-4000")

        test("2.15.789-Idea147-14", "2.15.789")

        test("1.0.0-alpha", "1.0.0-alpha")
        test("1.2.2123-alpha-023", "1.2.2123-alpha-023")
        test("1.0.0-release-IJ143-75", "1.0.0")
        test("1.0.2-eap-10-IJ141-10", "1.0.2-eap-10")
        test("1.0.2-dev-825", "1.0.2")
        test("1.0.1-rc-5-IJ143-7", "1.0.1-rc-5")

        test("1.1-M01-Studio2.2-9", "1.1-M01")
    }

    fun testOutdatedRuntime() {
        outdated("1.0.0-beta1-001-Idea141-12", "0.12.15")
        outdated("1.0.0-beta1-001-Idea141-1", "0.152.16")
        outdated("1.0.0-beta1-001-Idea141-1", "0.152.16")

        outdated("1.0.0-beta-1001-Idea141-12", "0.12.15")
        outdated("1.0.0-beta-1000-Idea141-1", "0.152.16")

        notOutdated("1.0.0-beta1-001-Idea141-12", "1.0.0-beta1-001")
        notOutdated("1.0.0-beta1-001-Idea143-14", "1.0.0-beta1-001")
        notOutdated("1.0.0-beta1-001", "1.0.0-beta1-001")
        notOutdated("1.0.0-beta1-001-Idea3-1", "1.0.0-beta1-001")
        notOutdated("1.0.0-beta1-001-Idea3-(1)", "1.0.0-beta1-001")

        notOutdated("1.0.0-release-IJ143-12", "1.0.0")
        notOutdated("1.0.1-release-IJ143-75", "1.0.1")

        outdated("1.0.0-beta1-002-Idea141-1", "1.0.0-beta1-001")
        outdated("1.0.0-beta1-010-Idea141-1", "1.0.0-beta1-009")
        outdated("1.0.0-beta1-100-Idea141-1", "1.0.0-beta1-099")
        outdated("1.0.0-beta2-000-Idea141-1", "1.0.0-beta1-999")
        outdated("1.1.0-beta1-000-Idea141-1", "1.0.9-beta9-999")
        outdated("2.0.0-beta1-000-Idea141-1", "1.9.9-beta9-999")

        outdated("1.0.0-beta-1001-Idea141-12", "1.0.0-beta-1000")
        outdated("1.0.0-beta-1010-Idea141-12", "1.0.0-beta-1009")
        outdated("1.0.0-beta-1100-Idea141-12", "1.0.0-beta-1099")
        outdated("1.0.0-beta-2000-Idea141-12", "1.0.0-beta-1999")
        outdated("1.0.1-beta-1001-Idea141-12", "1.0.0-beta-9999")
        outdated("1.1.0-beta-1001-Idea141-12", "1.0.9-beta-9999")
        outdated("2.0.0-beta-1001-Idea141-12", "1.9.9-beta-9999")
    }

    private fun outdated(plugin: String, library: String) {
        Assert.assertTrue("Should be outdated: plugin=$plugin, library=$library",
                isRuntimeOutdated(library, pluginRuntimeVersion(plugin)))
    }

    private fun notOutdated(plugin: String, library: String) {
        Assert.assertFalse("Should NOT be outdated: plugin=$plugin, library=$library",
                isRuntimeOutdated(library, pluginRuntimeVersion(plugin)))
    }

    private fun test(version: String, expected: String) {
        Assert.assertEquals(expected, pluginRuntimeVersion(version))
    }
}