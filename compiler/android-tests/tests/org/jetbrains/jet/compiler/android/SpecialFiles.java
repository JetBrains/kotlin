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

package org.jetbrains.jet.compiler.android;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Natalia.Ukhorskaya
 */

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
        filesCompiledWithoutStdLib.add("kt1953_class.kt"); // Exception in code
        filesCompiledWithoutStdLib.add("basicmethodSuperClass.jet"); // Exception in code
        filesCompiledWithoutStdLib.add("kt1980.kt"); // OVERLOAD_RESOLUTION_AMBIGUITY
        filesCompiledWithoutStdLib.add("kt503.jet"); // OVERLOAD_RESOLUTION_AMBIGUITY
        filesCompiledWithoutStdLib.add("kt504.jet"); // OVERLOAD_RESOLUTION_AMBIGUITY
        filesCompiledWithoutStdLib.add("kt772.jet"); // OVERLOAD_RESOLUTION_AMBIGUITY
        filesCompiledWithoutStdLib.add("kt773.jet"); // OVERLOAD_RESOLUTION_AMBIGUITY
        filesCompiledWithoutStdLib.add("kt796_797.jet"); // OVERLOAD_RESOLUTION_AMBIGUITY
        filesCompiledWithoutStdLib.add("kt950.jet"); // OVERLOAD_RESOLUTION_AMBIGUITY
        filesCompiledWithoutStdLib.add("kt2395.kt"); // With MOCK_JDK
    }

    private static void fillExcludedFiles() {
        excludedFiles.add("referencesStaticInnerClassMethod.kt"); // Must compile Java files before
        excludedFiles.add("referencesStaticInnerClassMethodL2.kt"); // Must compile Java files before
        excludedFiles.add("namespaceQualifiedMethod.jet"); // Cannot change package name
        excludedFiles.add("kt1482_2279.kt"); // Cannot change package name
        excludedFiles.add("kt1482.kt"); // Cannot change package name
        excludedFiles.add("importFromClassObject.jet"); // Cannot find usages in Codegen tests
        excludedFiles.add("withtypeparams.jet"); // Cannot find usages in Codegen tests
        excludedFiles.add("kt1113.kt"); // Commented
        excludedFiles.add("kt326.jet"); // Commented
        excludedFiles.add("kt694.jet"); // Commented
        excludedFiles.add("kt285.jet"); // Commented
        excludedFiles.add("kt857.jet"); // Commented
        excludedFiles.add("kt1120.kt"); // Commented
        excludedFiles.add("kt1213.kt"); // Commented
        excludedFiles.add("kt882.jet"); // Commented
        excludedFiles.add("kt789.jet"); // Commented
        excludedFiles.add("isTypeParameter.jet"); // Commented
        excludedFiles.add("nullability.jet"); // Commented
        excludedFiles.add("genericFunction.jet"); // Commented
        excludedFiles.add("forwardTypeParameter.jet"); // Commented
        excludedFiles.add("kt259.jet"); // Commented
        excludedFiles.add("classObjectMethod.jet"); // Commented
        excludedFiles.add("kt1592.kt"); // Codegen don't execute blackBoxFile() on it

        excludedFiles.add("box.kt"); // MultiFileTest not supported yet
        excludedFiles.add("kt2060_1.kt"); // MultiFileTest not supported yet

        excludedFiles.add("kt684.jet"); // StackOverflow with StringBuilder (escape())

        excludedFiles.add("kt1199.kt"); // Bug KT-2202
        excludedFiles.add("kt344.jet"); // Bug KT-2251
        excludedFiles.add("kt529.kt"); // Bug
    }

    private SpecialFiles() {
    }
}
