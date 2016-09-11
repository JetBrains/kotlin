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

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.script.KotlinScriptDefinitionProvider
import org.jetbrains.kotlin.script.ScriptParameter
import org.jetbrains.kotlin.scripts.SimpleParamsTestScriptDefinition
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.org.objectweb.asm.Opcodes

class ScriptGenTest : CodegenTestCase() {
    companion object {
        private val FIB_SCRIPT_DEFINITION = SimpleParamsTestScriptDefinition(
                ".lang.kt",
                listOf(ScriptParameter(Name.identifier("num"), DefaultBuiltIns.Instance.intType))
        )
        private val NO_PARAM_SCRIPT_DEFINITION = SimpleParamsTestScriptDefinition(
                ".kts",
                emptyList<ScriptParameter>()
        )
    }

    override fun setUp() {
        super.setUp()
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY)
        KotlinScriptDefinitionProvider.getInstance(myEnvironment.project).setScriptDefinitions(
                listOf(FIB_SCRIPT_DEFINITION, NO_PARAM_SCRIPT_DEFINITION)
        )
    }

    fun testLanguage() {
        loadFile("scriptCustom/fib.lang.kt")
        val aClass = generateClass("Fib")
        val constructor = aClass.getConstructor(Integer.TYPE)
        val result = aClass.getDeclaredField("result")
        result.isAccessible = true
        val script = constructor.newInstance(5)
        assertEquals(8, result.get(script))
    }

    fun testLanguageWithPackage() {
        loadFile("scriptCustom/fibwp.lang.kt")
        val aClass = generateClass("test.Fibwp")
        val constructor = aClass.getConstructor(Integer.TYPE)
        val result = aClass.getDeclaredField("result")
        result.isAccessible = true
        val script = constructor.newInstance(5)
        assertEquals(8, result.get(script))
    }

    fun testDependentScripts() {
        loadFiles("scriptCustom/fibwp.lang.kt", "scriptCustom/fibwprunner.kts")
        val aClass = generateClass("Fibwprunner")
        val constructor = aClass.getConstructor()
        val result = aClass.getDeclaredField("result")
        result.isAccessible = true
        val resultMethod = aClass.getDeclaredMethod("getResult")
        assertTrue(resultMethod.modifiers and Opcodes.ACC_FINAL != 0)
        assertTrue(resultMethod.modifiers and Opcodes.ACC_PUBLIC != 0)
        assertTrue(result.modifiers and Opcodes.ACC_PRIVATE != 0)
        val script = constructor.newInstance()
        assertEquals(8, result.get(script))
        assertEquals(8, resultMethod.invoke(script))
    }

    fun testScriptWhereMethodHasClosure() {
        loadFile("scriptCustom/methodWithClosure.lang.kt")
        val aClass = generateClass("MethodWithClosure")
        val constructor = aClass.getConstructor(Integer.TYPE)
        val script = constructor.newInstance(239)
        val fib = aClass.getMethod("method")
        val invoke = fib.invoke(script)
        assertEquals(239, invoke as Int / 2)
    }
}
