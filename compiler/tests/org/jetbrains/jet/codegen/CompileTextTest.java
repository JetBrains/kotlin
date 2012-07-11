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

import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.cli.jvm.compiler.K2JVMCompileEnvironmentConfiguration;
import org.jetbrains.jet.cli.jvm.compiler.KotlinToJVMBytecodeCompiler;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class CompileTextTest extends CodegenTestCase {
    public void testMe() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.addAll(JVMConfigurationKeys.CLASSPATH_KEY, getClassPathFromClassLoaders(getClass().getClassLoader()));
        myEnvironment = new JetCoreEnvironment(getTestRootDisposable(), compilerConfiguration);
        String text = "import org.jetbrains.jet.codegen.CompileTextTest; fun x() = CompileTextTest()";
        K2JVMCompileEnvironmentConfiguration configuration = new K2JVMCompileEnvironmentConfiguration(
                myEnvironment, MessageCollector.PLAIN_TEXT_TO_SYSTEM_ERR, false, BuiltinsScopeExtensionMode.ALL, false,
                BuiltinToJavaTypesMapping.ENABLED);
        ClassLoader classLoader = KotlinToJVMBytecodeCompiler.compileText(configuration, text);
        Class<?> namespace = classLoader.loadClass("namespace");
        Method x = namespace.getDeclaredMethod("x");
        Object invoke = x.invoke(null);
        assertTrue(invoke instanceof CompileTextTest);
    }

    private static List<File> getClassPathFromClassLoaders(ClassLoader loader) {
        List<File> list = new ArrayList<File>();
        addClassPathFromClassLoadersRecursively(loader, list);
        return list;
    }

    private static void addClassPathFromClassLoadersRecursively(ClassLoader loader, List<File> list) {
        ClassLoader parent = loader.getParent();
        if (parent != null) {
            addClassPathFromClassLoadersRecursively(parent, list);
        }

        if (loader instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader) loader).getURLs()) {
                File file = new File(url.getPath());
                if (file.exists() && (!file.isFile() || file.getPath().endsWith(".jar"))) {
                    list.add(file);
                }
            }
        }
    }

}
