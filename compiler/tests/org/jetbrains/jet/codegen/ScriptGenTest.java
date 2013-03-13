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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.lang.parsing.JetScriptDefinition;
import org.jetbrains.jet.lang.parsing.JetScriptDefinitionProvider;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.ScriptNameUtil;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ScriptGenTest extends CodegenTestCase {
    protected Object scriptInstance;

    public static final JetScriptDefinition FIB_SCRIPT_DEFINITION =
            new JetScriptDefinition(".lang.kt", new AnalyzerScriptParameter("num", "jet.Int"));

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    @Override
    protected void tearDown() throws Exception {
        scriptInstance = null;
        super.tearDown();
    }

    private void blackBoxScript(String filename) {
        loadFile(filename);

        ClassFileFactory factory = generateClassesInFile();

        GeneratedClassLoader loader = createClassLoader(factory);

        String scriptClassName = ScriptNameUtil.classNameForScript(myFiles.getPsiFile());

        try {
            Class<?> scriptClass = loader.loadClass(scriptClassName);

            Constructor constructor = getConstructor(scriptClass, factory.getState().getScriptCodegen().getScriptConstructorMethod());
            scriptInstance = constructor.newInstance(myFiles.getScriptParameterValues().toArray());

            assertFalse("expecting at least one expectation", myFiles.getExpectedValues().isEmpty());

            for (Pair<String, String> nameValue : myFiles.getExpectedValues()) {
                String fieldName = nameValue.first;
                String expectedValue = nameValue.second;

                if (expectedValue.equals("<nofield>")) {
                    try {
                        scriptClass.getDeclaredField(fieldName);
                        fail("must have no field " + fieldName);
                    } catch (NoSuchFieldException e) {
                        continue;
                    }
                }

                Field field = scriptClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object result = field.get(scriptInstance);
                String resultString = result != null ? result.toString() : "null";
                assertEquals("comparing field " + fieldName, expectedValue, resultString);
            }
        }
        catch (Throwable e) {
            System.out.println(generateToText());
            ExceptionUtils.rethrow(e);
        }
    }

    protected Constructor getConstructor(@NotNull Class<?> clazz, org.jetbrains.asm4.commons.Method method) {
        if (!method.getName().equals("<init>")) {
            throw new IllegalArgumentException("not constructor: " + method);
        }
        Class[] classes = new Class[method.getArgumentTypes().length];
        for (int i = 0; i < classes.length; ++i) {
            classes[i] = loadClassFromType(method.getArgumentTypes()[i]);
        }
        try {
            return clazz.getConstructor(classes);
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private Class<?> loadClassFromType(@NotNull Type type) {
        try {
            switch (type.getSort()) {
                case Type.OBJECT:
                    return Class.forName(type.getClassName());
                case Type.INT:
                    return int.class;
                case Type.LONG:
                    return long.class;
                default:
                    // AFAIK there is no way to create array class from class
                    if (type.getDescriptor().equals("[Ljava/lang/String;")) {
                        return String[].class;
                    }
                    throw new IllegalStateException("not implemented: " + type.getDescriptor());
            }
        }
        catch (Exception e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    public void testHelloWorld() {
        blackBoxScript("script/helloWorld.ktscript");
    }

    public void testString() {
        blackBoxScript("script/string.ktscript");
    }

    public void testTopLevelFunction() throws Exception {
        blackBoxScript("script/topLevelFunction.ktscript");
        Method method = scriptInstance.getClass().getMethod("factorial", new Class<?>[]{ int.class });
        Object r = method.invoke(scriptInstance, 4);
        assertEquals(24, r);
    }

    public void testTopLevelFunctionClosure() {
        blackBoxScript("script/topLevelFunctionClosure.ktscript");
    }

    public void testSecondLevelFunction() {
        blackBoxScript("script/secondLevelFunction.ktscript");
    }

    public void testSecondLevelFunctionClosure() {
        blackBoxScript("script/secondLevelFunctionClosure.ktscript");
    }

    public void testSecondLevelVal() {
        blackBoxScript("script/secondLevelVal.ktscript");
    }

    public void testTopLevelProperty() {
        blackBoxScript("script/topLevelProperty.ktscript");
    }

    public void testScriptParameter() {
        blackBoxScript("script/parameter.ktscript");
    }

    public void testScriptParameterLong() {
        blackBoxScript("script/parameterLong.ktscript");
    }

    public void testScriptParameterArray() {
        blackBoxScript("script/parameterArray.ktscript");
    }

    public void testScriptParameterClosure() {
        blackBoxScript("script/parameterClosure.ktscript");
    }

    public void testEmpty() {
        blackBoxScript("script/empty.ktscript");
    }

    public void testLanguage() {
        JetScriptDefinitionProvider.getInstance(myEnvironment.getProject()).addScriptDefinition(FIB_SCRIPT_DEFINITION);
        loadFile("script/fib.lang.kt");
        Class aClass = generateClass("Fib");
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
        Class aClass = generateClass("test.Fibwp");
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
        Class aClass = generateClass("Fibwprunner");
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
        Class aClass = generateClass("MethodWithClosure");
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
