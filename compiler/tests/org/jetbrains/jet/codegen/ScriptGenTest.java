/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.components.ServiceManager;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.lang.parsing.JetScriptDefinition;
import org.jetbrains.jet.lang.parsing.JetScriptDefinitionProvider;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ScriptGenTest extends CodegenTestCase {

    public static final JetScriptDefinition FIB_SCRIPT_DEFINITION =
            new JetScriptDefinition(".lang.kt", new AnalyzerScriptParameter("num", "jet.Int"));

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testHelloWorld() {
        blackBoxFile("script/helloWorld.ktscript");
    }

    public void testString() {
        blackBoxFile("script/string.ktscript");
    }

    public void testTopLevelFunction() throws Exception {
        blackBoxFile("script/topLevelFunction.ktscript");
        Method method = scriptInstance.getClass().getMethod("factorial", new Class<?>[]{ int.class });
        Object r = method.invoke(scriptInstance, 4);
        assertEquals(24, r);
    }

    public void testTopLevelFunctionClosure() {
        blackBoxFile("script/topLevelFunctionClosure.ktscript");
    }

    public void testSecondLevelFunction() {
        blackBoxFile("script/secondLevelFunction.ktscript");
    }

    public void testSecondLevelFunctionClosure() {
        blackBoxFile("script/secondLevelFunctionClosure.ktscript");
    }

    public void testSecondLevelVal() {
        blackBoxFile("script/secondLevelVal.ktscript");
    }

    public void testTopLevelProperty() {
        blackBoxFile("script/topLevelProperty.ktscript");
    }

    public void testScriptParameter() {
        blackBoxFile("script/parameter.ktscript");
    }

    public void testScriptParameterLong() {
        blackBoxFile("script/parameterLong.ktscript");
    }

    public void testScriptParameterArray() {
        blackBoxFile("script/parameterArray.ktscript");
    }

    public void testScriptParameterClosure() {
        blackBoxFile("script/parameterClosure.ktscript");
    }

    public void testEmpty() {
        blackBoxFile("script/empty.ktscript");
    }

    public void testLanguage() {
        JetScriptDefinitionProvider.getInstance(myEnvironment.getProject()).addScriptDefinition(FIB_SCRIPT_DEFINITION);
        loadFile("script/fib.lang.kt");
        final Class aClass = loadClass("Fib", generateClassesInFile());
        try {
            Constructor constructor = aClass.getConstructor(int.class);
            Field result = aClass.getDeclaredField("result");
            result.setAccessible(true);
            Object script = constructor.newInstance(5);
            assertEquals(8,result.get(script));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testLanguageWithPackage() {
        JetScriptDefinitionProvider.getInstance(myEnvironment.getProject()).addScriptDefinition(FIB_SCRIPT_DEFINITION);
        loadFile("script/fibwp.lang.kt");
        final Class aClass = loadClass("test.Fibwp", generateClassesInFile());
        try {
            Constructor constructor = aClass.getConstructor(int.class);
            Field result = aClass.getDeclaredField("result");
            result.setAccessible(true);
            Object script = constructor.newInstance(5);
            assertEquals(8,result.get(script));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testDependentScripts() {
        JetScriptDefinitionProvider.getInstance(myEnvironment.getProject()).addScriptDefinition(FIB_SCRIPT_DEFINITION);
        loadFiles("script/fibwp.lang.kt", "script/fibwprunner.ktscript");
        final Class aClass = loadClass("Fibwprunner", generateClassesInFile());
        try {
            Constructor constructor = aClass.getConstructor();
            Field result = aClass.getDeclaredField("result");
            result.setAccessible(true);
            Method resultMethod = aClass.getDeclaredMethod("getResult");
            assertTrue((resultMethod.getModifiers() & Opcodes.ACC_FINAL) != 0);
            assertTrue((resultMethod.getModifiers() & Opcodes.ACC_PUBLIC) != 0);
            Field rv = aClass.getField("rv");
            assertTrue((result.getModifiers() & Opcodes.ACC_PRIVATE) != 0);
            Object script = constructor.newInstance();
            assertEquals(12,rv.get(script));
            assertEquals(8,result.get(script));
            assertEquals(8,resultMethod.invoke(script));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testScriptWhereMethodHasClosure() {
        JetScriptDefinitionProvider.getInstance(myEnvironment.getProject()).addScriptDefinition(FIB_SCRIPT_DEFINITION);
        loadFile("script/methodWithClosure.lang.kt");
        final Class aClass = loadClass("MethodWithClosure", generateClassesInFile());
        try {
            Constructor constructor = aClass.getConstructor(int.class);
            Object script = constructor.newInstance(239);
            Method fib = aClass.getMethod("method");
            Object invoke = fib.invoke(script);
            assertEquals(239, ((Integer)invoke)/2);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
