/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import androidx.compose.Composer
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import java.net.URLClassLoader

abstract class AbstractLoweringTests : AbstractCodegenTest() {

    fun codegen(text: String, dumpClasses: Boolean = false) {
        codegenNoImports(
            """
           import android.content.Context
           import android.widget.*
           import androidx.compose.*

           $text

        """, dumpClasses)
    }

    fun codegenNoImports(text: String, dumpClasses: Boolean = false) {
        val className = "Test_${uniqueNumber++}"
        val fileName = "$className.kt"

        classLoader(text, fileName, dumpClasses)
    }

    fun compose(
        supportingCode: String,
        composeCode: String,
        valuesFactory: () -> Map<String, Any> = { emptyMap() },
        dumpClasses: Boolean = false
    ): RobolectricComposeTester {
        val className = "TestFCS_${uniqueNumber++}"
        val fileName = "$className.kt"

        val candidateValues = valuesFactory()

        @Suppress("NO_REFLECTION_IN_CLASS_PATH")
        val parameterList = candidateValues.map {
            if (it.key.contains(':')) {
                it.key
            } else "${it.key}: ${it.value::class.qualifiedName}"
        }.joinToString()
        val parameterTypes = candidateValues.map {
            it.value::class.javaPrimitiveType ?: it.value::class.javaObjectType
        }.toTypedArray()

        val compiledClasses = classLoader(
            """
       import android.content.Context
       import android.widget.*
       import androidx.compose.*
       import androidx.ui.androidview.adapters.*

       $supportingCode

       class $className {

         @Composable
         fun test($parameterList) {
           $composeCode
         }
       }
    """, fileName, dumpClasses
        )

        val allClassFiles = compiledClasses.allGeneratedFiles.filter {
            it.relativePath.endsWith(".class")
        }

        val loader = URLClassLoader(emptyArray(), this.javaClass.classLoader)

        val instanceClass = run {
            var instanceClass: Class<*>? = null
            var loadedOne = false
            for (outFile in allClassFiles) {
                val bytes = outFile.asByteArray()
                val loadedClass = loadClass(loader, null, bytes)
                if (loadedClass.name == className) instanceClass = loadedClass
                loadedOne = true
            }
            if (!loadedOne) error("No classes loaded")
            instanceClass ?: error("Could not find class $className in loaded classes")
        }

        val instanceOfClass = instanceClass.newInstance()
        val testMethod = instanceClass.getMethod(
            "test",
            *parameterTypes,
            Composer::class.java,
            Int::class.java,
            Int::class.java
        )

        return compose { composer, _, _ ->
            val values = valuesFactory()
            val arguments = values.map { it.value }.toTypedArray()
            testMethod.invoke(instanceOfClass, *arguments, composer, 0, 0)
        }
    }

    private fun ResolvedCall<*>.isEmit(): Boolean = candidateDescriptor is ComposableEmitDescriptor
    private fun ResolvedCall<*>.isCall(): Boolean =
        candidateDescriptor is ComposableFunctionDescriptor

    private val callPattern = Regex("(<normal>)|(<emit>)|(<call>)")
    private fun extractCarets(text: String): Pair<String, List<Pair<Int, String>>> {
        val indices = mutableListOf<Pair<Int, String>>()
        var offset = 0
        val src = callPattern.replace(text) {
            indices.add(it.range.first - offset to it.value)
            offset += it.range.last - it.range.first + 1
            ""
        }
        return src to indices
    }
}