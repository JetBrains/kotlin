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

import junit.framework.TestCase;
import org.jetbrains.jet.compiler.CompileEnvironment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CompileTextTest extends CodegenTestCase {
    public void testMe() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String text = "import org.jetbrains.jet.codegen.CompileTextTest; fun x() = CompileTextTest()";
        CompileEnvironment compileEnvironment = new CompileEnvironment();
        compileEnvironment.getMyEnvironment().addToClasspathFromClassLoader(getClass().getClassLoader());
        ClassLoader classLoader = compileEnvironment.compileText(text);
        Class<?> namespace = classLoader.loadClass("namespace");
        Method x = namespace.getDeclaredMethod("x");
        Object invoke = x.invoke(null);
        assertTrue(invoke instanceof CompileTextTest);
    }
}
