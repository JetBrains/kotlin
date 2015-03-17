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
import org.jetbrains.kotlin.idea.versions.KotlinRuntimeLibraryUtil
import org.jetbrains.kotlin.idea.versions.OutdatedKotlinRuntimeNotification
import org.junit.Assert


public class KotlinRuntimeLibraryUtilTest : TestCase() {
    public fun testKotlinLibraryRelevantVersion() {
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
    }

    private fun test(version: String, expected: String) {
        Assert.assertEquals(expected, KotlinRuntimeLibraryUtil.bundledRuntimeVersion(version))
    }
}