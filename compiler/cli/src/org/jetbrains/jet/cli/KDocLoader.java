/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.jet.cli;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.Disposable;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.compiler.CompilerPlugin;
import org.jetbrains.jet.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;

/**
 * A simple facade to auto-detect the KDoc processor if its available on the classpath
 */
public class KDocLoader {

    private final String outputDir;

    public KDocLoader(String outputDir) {
        this.outputDir = outputDir;
    }

    public CompilerPlugin createCompilerPlugin() {
        // lets see if we can see the KDoc class
        String name = "org.jetbrains.kotlin.doc.KDoc";
        Class<?> aClass = null;
        try {
            aClass = loadClass(name);
        } catch (ClassNotFoundException e) {
            System.out.println("Could not find class: " + name);
            return null;
        }
        if (aClass != null) {
            try {
                File dir = new File(outputDir);
                Constructor<?> constructor = aClass.getConstructor(File.class);
                if (constructor != null) {
                    return (CompilerPlugin) constructor.newInstance(dir);
                }
            } catch (Exception e) {
                System.out.println("Failed to create Processor: " + e);
            }
        }
        return null;
    }

    public static Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            try {
                return Thread.currentThread().getContextClassLoader().loadClass(name);
            } catch (ClassNotFoundException e1) {
                return KDocLoader.class.getClassLoader().loadClass(name);
            }
        }
    }

    /**
     * Installs the KDoc compiler plugin if it can be created
     */
    public static boolean install(String docOutputDir, JetCoreEnvironment environment) {
        KDocLoader loader = new KDocLoader(docOutputDir);
        CompilerPlugin processor = loader.createCompilerPlugin();
        if (processor != null) {
            environment.getCompilerPlugins().add(processor);
            return true;
        } else {
            return false;
        }
    }
}
