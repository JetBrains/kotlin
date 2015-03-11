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

package org.jetbrains.kotlin.resolve.annotation

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil

class AnnotationDescriptorResolveWithoutAnalyzeBodyTest : AnnotationDescriptorResolveTest() {

    override fun doTest(content: String, expectedAnnotation: String) {
        checkAnnotationOnAllExceptLocalDeclarations(content, expectedAnnotation)
    }

    override fun analyzeFile(ktFile: JetFile): AnalysisResult = JvmResolveUtil.analyzeFilesWithJavaIntegration(ktFile.getProject(), setOf(ktFile));
}
