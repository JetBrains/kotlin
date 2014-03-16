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
import org.jetbrains.jet.JetTestUtils
import org.jetbrains.jet.util.slicedmap.WritableSlice
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant
import org.jetbrains.jet.lang.resolve.constants.StringValue
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.lang.resolve.constants.IntegerValueConstant

abstract class AbstractEvaluateExpressionTest : AbstractAnnotationDescriptorResolveTest() {

    // Test directives should look like [// val testedPropertyName: expectedValue]
    fun doConstantTest(path: String) {
        doTest(path) {
            property, context ->
            val compileTimeConstant = property.getCompileTimeInitializer()
            if (compileTimeConstant is StringValue) {
                "\\\"${compileTimeConstant.getValue()}\\\""
            } else {
                compileTimeConstant.toString()
            }
        }
    }

    // Test directives should look like [// val testedPropertyName: expectedValue]
    fun doIsPureTest(path: String) {
        doTest(path) {
            property, context ->
            val compileTimeConstant = property.getCompileTimeInitializer()
            if (compileTimeConstant is IntegerValueConstant) {
                compileTimeConstant.isPure().toString()
            } else {
                "null"
            }
        }
    }

    private fun doTest(path: String, getValueToTest: (VariableDescriptor, BindingContext) -> String) {
        val myFile = File(path)
        val fileText = FileUtil.loadFile(myFile, true)
        val packageView = getPackage(fileText)

        val propertiesForTest = getObjectsToTest(fileText)

        val expectedActual = arrayListOf<Pair<String, String>>()

        for (propertyName in propertiesForTest) {
            val expectedPropertyPrefix = "// val ${propertyName}: "
            val expected = InTextDirectivesUtils.findStringWithPrefixes(fileText, expectedPropertyPrefix)
            assertNotNull(expected, "Failed to find expected directive: $expectedPropertyPrefix")

            val property = AbstractAnnotationDescriptorResolveTest.getPropertyDescriptor(packageView, propertyName, false)
            ?: AbstractAnnotationDescriptorResolveTest.getLocalVarDescriptor(context!!, propertyName)

            val testedObject = getValueToTest(property, context!!)
            expectedActual.add(expectedPropertyPrefix + expected!! to expectedPropertyPrefix + testedObject)
        }

        var actualFileText = fileText
        for ((expected, actual) in expectedActual) {
            assert(actualFileText.contains(expected), "File text should contains $expected")
            actualFileText = actualFileText.replace(expected, actual)
        }

        JetTestUtils.assertEqualsToFile(myFile, actualFileText)
    }

    fun getObjectsToTest(fileText: String): List<String> {
        return InTextDirectivesUtils.findListWithPrefixes(fileText, "// val").map {
            val matcher = pattern.matcher(it)
            if (matcher.find()) {
                matcher.group(0) ?: "Couldn't match tested object $it"
            } else "Couldn't match tested object $it"
        }
    }

    class object {
        val pattern = Pattern.compile(".+(?=:)")
    }
}
