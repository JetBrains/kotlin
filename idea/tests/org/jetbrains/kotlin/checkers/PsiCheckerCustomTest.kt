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

package org.jetbrains.kotlin.checkers

import com.intellij.codeInspection.ex.EntryPointsManagerBase

class PsiCheckerCustomTest : AbstractPsiCheckerTest() {
    fun testNoUnusedParameterWhenCustom() {
        val testAnnotation = "MyTestAnnotation"
        EntryPointsManagerBase.getInstance(project).ADDITIONAL_ANNOTATIONS.add(testAnnotation)
        try {
            doTest("idea/testData/checker/custom/noUnusedParameterWhenCustom.kt")
        }
        finally {
            EntryPointsManagerBase.getInstance(project).ADDITIONAL_ANNOTATIONS.remove(testAnnotation)
        }
    }
}