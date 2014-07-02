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

package org.jetbrains.jet.compiler.android;

import com.google.common.collect.Sets;

import java.util.Set;


public class SpecialFiles {
    private static final Set<String> excludedFiles = Sets.newHashSet();
    private static final Set<String> filesCompiledWithoutStdLib = Sets.newHashSet();

    static {
        fillExcludedFiles();
        fillFilesCompiledWithoutStdLib();
    }


    public static Set<String> getExcludedFiles() {
        return excludedFiles;
    }

    public static Set<String> getFilesCompiledWithoutStdLib() {
        return filesCompiledWithoutStdLib;
    }

    private static void fillFilesCompiledWithoutStdLib() {
        filesCompiledWithoutStdLib.add("kt1980.kt");
        filesCompiledWithoutStdLib.add("kt1953_class.kt"); // Exception in code
        filesCompiledWithoutStdLib.add("basicmethodSuperClass.kt"); // Exception in code
        filesCompiledWithoutStdLib.add("kt503.kt"); // OVERLOAD_RESOLUTION_AMBIGUITY
        filesCompiledWithoutStdLib.add("kt504.kt"); // OVERLOAD_RESOLUTION_AMBIGUITY
        filesCompiledWithoutStdLib.add("kt772.kt"); // OVERLOAD_RESOLUTION_AMBIGUITY
        filesCompiledWithoutStdLib.add("kt773.kt"); // OVERLOAD_RESOLUTION_AMBIGUITY
        filesCompiledWithoutStdLib.add("kt796_797.kt"); // OVERLOAD_RESOLUTION_AMBIGUITY
        filesCompiledWithoutStdLib.add("kt950.kt"); // OVERLOAD_RESOLUTION_AMBIGUITY
        filesCompiledWithoutStdLib.add("kt3190.kt"); // OVERLOAD_RESOLUTION_AMBIGUITY
        filesCompiledWithoutStdLib.add("kt4265.kt"); // OVERLOAD_RESOLUTION_AMBIGUITY
        filesCompiledWithoutStdLib.add("realStringRepeat.kt"); // OVERLOAD_RESOLUTION_AMBIGUITY
        filesCompiledWithoutStdLib.add("kt2395.kt"); // With MOCK_JDK
    }

    private static void fillExcludedFiles() {
        excludedFiles.add("boxAgainstJava");  // Must compile Java files before
        excludedFiles.add("boxWithJava");  // Must compile Java files before
        excludedFiles.add("boxMultiFile"); // MultiFileTest not supported yet
        excludedFiles.add("boxInline"); // MultiFileTest not supported yet

        excludedFiles.add("reflection");
        excludedFiles.add("kt3238.kt"); // Reflection
        excludedFiles.add("kt1482_2279.kt"); // Reflection

        excludedFiles.add("nestedInPackage.kt"); // Cannot change package name
        excludedFiles.add("importNestedClass.kt"); // Cannot change package name
        excludedFiles.add("packageQualifiedMethod.kt"); // Cannot change package name
        excludedFiles.add("classObjectToString.kt"); // Cannot change package name

        excludedFiles.add("kt326.kt"); // Commented
        excludedFiles.add("kt1213.kt"); // Commented

        excludedFiles.add("kt684.kt"); // StackOverflow with StringBuilder (escape())

        excludedFiles.add("kt529.kt");  // Bug
        excludedFiles.add("kt344.kt");  // Bug

        excludedFiles.add("comparisonWithNullCallsFun.kt"); // java.lang.NoClassDefFoundError: jet.Nothing
        excludedFiles.add("kt3574.kt"); // java.lang.NoClassDefFoundError: jet.Nothing

        excludedFiles.add("genericBackingFieldSignature.kt"); // Wrong signature after package renaming
        excludedFiles.add("genericMethodSignature.kt"); // Wrong signature after package renaming

        excludedFiles.add("classpath.kt"); // Some classes are not visible on android
    }

    private SpecialFiles() {
    }
}
