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

package org.jetbrains.kotlin.codegen;

import org.jetbrains.kotlin.builtins.DefaultBuiltIns;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.script.KotlinScriptDefinition;
import org.jetbrains.kotlin.script.KotlinScriptDefinitionProvider;
import org.jetbrains.kotlin.script.ScriptParameter;
import org.jetbrains.kotlin.scripts.SimpleParamsTestScriptDefinition;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import static java.util.Collections.singletonList;

public class ScriptGenTest extends CodegenTestCase {
    private static final KotlinScriptDefinition FIB_SCRIPT_DEFINITION =
            new SimpleParamsTestScriptDefinition(
                    ".lang.kt",
                    singletonList(new ScriptParameter(Name.identifier("num"), DefaultBuiltIns.getInstance().getIntType()))
            );
    private static final KotlinScriptDefinition NO_PARAM_SCRIPT_DEFINITION =
            new SimpleParamsTestScriptDefinition(
                    ".kts",
                    Collections.<ScriptParameter>emptyList()
            );

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        KotlinScriptDefinitionProvider.getInstance(myEnvironment.getProject()).setScriptDefinitions(
                Arrays.asList(FIB_SCRIPT_DEFINITION, NO_PARAM_SCRIPT_DEFINITION)
        );
    }

    public void testLanguage() throws Exception {
        loadFile("scriptCustom/fib.lang.kt");
        Class<?> aClass = generateClass("Fib");
        Constructor constructor = aClass.getConstructor(int.class);
        Field result = aClass.getDeclaredField("result");
        result.setAccessible(true);
        Object script = constructor.newInstance(5);
        assertEquals(8, result.get(script));
    }

    public void testLanguageWithPackage() throws Exception {
        loadFile("scriptCustom/fibwp.lang.kt");
        Class<?> aClass = generateClass("test.Fibwp");
        Constructor constructor = aClass.getConstructor(int.class);
        Field result = aClass.getDeclaredField("result");
        result.setAccessible(true);
        Object script = constructor.newInstance(5);
        assertEquals(8, result.get(script));
    }

    public void testDependentScripts() throws Exception {
        loadFiles("scriptCustom/fibwp.lang.kt", "scriptCustom/fibwprunner.kts");
        Class<?> aClass = generateClass("Fibwprunner");
        Constructor constructor = aClass.getConstructor();
        Field result = aClass.getDeclaredField("result");
        result.setAccessible(true);
        Method resultMethod = aClass.getDeclaredMethod("getResult");
        assertTrue((resultMethod.getModifiers() & Opcodes.ACC_FINAL) != 0);
        assertTrue((resultMethod.getModifiers() & Opcodes.ACC_PUBLIC) != 0);
        assertTrue((result.getModifiers() & Opcodes.ACC_PRIVATE) != 0);
        Object script = constructor.newInstance();
        assertEquals(8, result.get(script));
        assertEquals(8, resultMethod.invoke(script));
    }

    public void testScriptWhereMethodHasClosure() throws Exception {
        loadFile("scriptCustom/methodWithClosure.lang.kt");
        Class<?> aClass = generateClass("MethodWithClosure");
        Constructor constructor = aClass.getConstructor(int.class);
        Object script = constructor.newInstance(239);
        Method fib = aClass.getMethod("method");
        Object invoke = fib.invoke(script);
        assertEquals(239, ((Integer) invoke) / 2);
    }
}
