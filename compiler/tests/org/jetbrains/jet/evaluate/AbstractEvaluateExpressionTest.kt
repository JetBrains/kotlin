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

package org.jetbrains.jet.evaluate

import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.resolve.annotation.AbstractAnnotationDescriptorResolveTest
import java.io.File
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jet.lang.resolve.BindingContextUtils
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.InTextDirectivesUtils
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import java.util.regex.Pattern
import org.intellij.lang.annotations.RegExp
import com.intellij.openapi.util.text.StringUtil

abstract class AbstractEvaluateExpressionTest: AbstractAnnotationDescriptorResolveTest() {

    // Test directives should look like [// val testedPropertyName: expectedValue]
    fun doTest(path: String) {
        val fileText = FileUtil.loadFile(File(path))
        val namespaceDescriptor = getNamespaceDescriptor(fileText)

        val propertiesForTest = getObjectsToTest(fileText)

        for (propertyName in propertiesForTest) {
            val property = AbstractAnnotationDescriptorResolveTest.getPropertyDescriptor(namespaceDescriptor, propertyName)
            val jetProperty = BindingContextUtils.descriptorToDeclaration(context!!, property) as JetProperty
            val compileTimeConstant = context!!.get(BindingContext.COMPILE_TIME_VALUE, jetProperty.getInitializer())

            val expected = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// val ${propertyName}: ")
            assertNotNull(expected, "Failed to find expected directive: // val ${propertyName}: ")
            assertEquals(expected, StringUtil.unquoteString(compileTimeConstant.toString()), "Failed for $propertyName")
        }
    }

    fun getObjectsToTest(fileText: String): List<String> {
        return InTextDirectivesUtils.findListWithPrefixes(fileText, "// val").map {
            val matcher = pattern.matcher(it)
            if (matcher.find()) {
                matcher.group(0) ?: "Couldn't match tested object $it"
            }
            else "Couldn't match tested object $it"
        }
    }

    class object {
        val pattern = Pattern.compile(".+(?=:)")
    }
}
