/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.highlighter

import com.intellij.openapi.util.io.FileUtil
import java.io.File
import com.intellij.rt.execution.junit.FileComparisonFailure
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import org.jetbrains.jet.testing.TagsTestDataUtil
import org.jetbrains.jet.InTextDirectivesUtils

public class CustomHighlightingTest : AbstractHighlightingTest() {

    //TODO:
    val path = "idea/testData/customHighlighting/objectLiteralInPropertyInitializer.kt"

    fun testObjectLiteralInPropertyInitializer() {
        myFixture.configureByFile(path)
        myFixture.checkHighlighting(true, false, false)
        myFixture.type("println()")
        myFixture.checkHighlighting(true, false, false)
    }
}
