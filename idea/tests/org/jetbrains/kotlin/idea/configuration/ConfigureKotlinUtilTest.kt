/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.testFramework.UsefulTestCase
import org.junit.Assert

class ConfigureKotlinUtilTest : UsefulTestCase() {
    fun test11Prerelease() {
        Assert.assertTrue(useEap11Repository("1.1-M04"))
        Assert.assertTrue(useEap11Repository("1.1-beta"))
        Assert.assertTrue(useEap11Repository("1.1.0-beta"))
        Assert.assertTrue(useEap11Repository("1.1-beta-2"))
        Assert.assertTrue(useEap11Repository("1.1-rc2"))
        Assert.assertTrue(useEap11Repository("1.1.0-RC"))
        Assert.assertTrue(useEap11Repository("1.1.1-eap-22"))
        Assert.assertFalse(useEap11Repository("1.1"))
        Assert.assertFalse(useEap11Repository("1.1.2"))
        Assert.assertFalse(useEap11Repository("1.1.2-3"))
        Assert.assertFalse(useEap11Repository("1.1.0-dev-1234"))
    }
}