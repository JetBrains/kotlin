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

package org.jetbrains.kotlin.generators.test.evaluate

import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.generators.evaluate
import org.jetbrains.kotlin.test.JetTestUtils
import org.junit.Ignore

Ignore public class GenerateOperationsMapTest : UsefulTestCase() {
    public fun testGeneratedDataIsUpToDate(): Unit {
        val text = evaluate.generate()
        JetTestUtils.assertEqualsToFile(evaluate.DEST_FILE, text)
    }
}
