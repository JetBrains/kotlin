/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.resolve.annotation

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jet.InTextDirectivesUtils
import java.io.File
import org.jetbrains.jet.JetTestUtils

public abstract class AbstractAnnotationParameterTest : AbstractAnnotationDescriptorResolveTest() {
    fun doTest(path: String) {
        val fileText = FileUtil.loadFile(File(path))
        val packageView = getPackage(fileText)
        val classDescriptor = AbstractAnnotationDescriptorResolveTest.getClassDescriptor(packageView, "MyClass")

        val expected = InTextDirectivesUtils.findListWithPrefixes(fileText, "// EXPECTED: ").makeString(", ")
        val actual = AbstractAnnotationDescriptorResolveTest.getAnnotations(classDescriptor)

        JetTestUtils.assertEqualsToFile(File(path), fileText.replace(expected, actual))
    }
}
