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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import java.io.File

@OptIn(ObsoleteTestInfrastructure::class)
abstract class AbstractBlackBoxInlineCodegenTest : AbstractBlackBoxCodegenTest() {
    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        super.doMultiFileTest(wholeFile, files)
        try {
            InlineTestUtil.checkNoCallsToInline(
                initializedClassLoader.allGeneratedFiles.filterClassFiles(),
                skipParameterCheckingInDirectives = files.any { "NO_CHECK_LAMBDA_INLINING" in it.directives },
                skippedMethods = files.flatMapTo(mutableSetOf()) { it.directives.listValues("SKIP_INLINE_CHECK_IN") ?: emptyList() }
            )
            SMAPTestUtil.checkSMAP(files, generateClassesInFile().getClassFiles(), false)
        } catch (e: Throwable) {
            println(generateToText())
            throw e
        }
    }
}
