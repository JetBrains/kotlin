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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;


public class SpecialFiles {
    private static final Set<String> excludedFiles = Sets.newHashSet();
    private static final Set<String> filesCompiledWithoutStdLib = Sets.newHashSet();
    private static final Set<String> filesCompiledWithJUnit = Sets.newHashSet();
    private static final Map<String, String> filesWithSpecialResult = Maps.newHashMap();

    static {
        fillExcludedFiles();
        fillFilesCompiledWithoutStdLib();
        fillFilesCompiledWithJUnit();
        fillFilesWithSpecialResult();
    }


    public static Set<String> getFilesCompiledWithJUnit() {
        return filesCompiledWithJUnit;
    }

    public static Set<String> getExcludedFiles() {
        return excludedFiles;
    }

    public static Set<String> getFilesCompiledWithoutStdLib() {
        return filesCompiledWithoutStdLib;
    }

    public static Map<String, String> getFilesWithSpecialResult() {
        return filesWithSpecialResult;
    }

    private static void fillFilesWithSpecialResult() {
        filesWithSpecialResult.put("kt2398.kt", "OKKO");
    }

    private static void fillFilesCompiledWithJUnit() {
        filesCompiledWithJUnit.add("kt2334.kt");
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
        filesCompiledWithoutStdLib.add("kt2395.kt"); // With MOCK_JDK
    }

    private static void fillExcludedFiles() {
        excludedFiles.add("kt2781.kt"); // Must compile Java files before
        excludedFiles.add("simpleJavaInnerEnum.kt"); // Must compile Java files before
        excludedFiles.add("referencesStaticInnerClassMethod.kt"); // Must compile Java files before
        excludedFiles.add("referencesStaticInnerClassMethodL2.kt"); // Must compile Java files before
        excludedFiles.add("simpleJavaEnum.kt"); // Must compile Java files before
        excludedFiles.add("simpleJavaEnumWithFunction.kt"); // Must compile Java files before
        excludedFiles.add("simpleJavaEnumWithStaticImport.kt"); // Must compile Java files before
        excludedFiles.add("removeInIterator.kt"); // Must compile Java files before
        excludedFiles.add("kt3238.kt"); // Reflection
        excludedFiles.add("namespaceQualifiedMethod.kt"); // Cannot change package name
        excludedFiles.add("kt1482_2279.kt"); // Cannot change package name
        excludedFiles.add("kt1482.kt"); // Cannot change package name
        excludedFiles.add("withtypeparams.kt"); // Cannot find usages in Codegen tests
        excludedFiles.add("kt326.kt"); // Commented
        excludedFiles.add("kt1213.kt"); // Commented
        excludedFiles.add("kt882.kt"); // Commented
        excludedFiles.add("kt789.kt"); // Commented
        excludedFiles.add("enum.kt"); // Commented
        excludedFiles.add("withclosure.kt"); // Commented
        excludedFiles.add("isTypeParameter.kt"); // Commented
        excludedFiles.add("nullability.kt"); // Commented
        excludedFiles.add("genericFunction.kt"); // Commented
        excludedFiles.add("forwardTypeParameter.kt"); // Commented
        excludedFiles.add("classObjectMethod.kt"); // Commented

        excludedFiles.add("inRangeConditionsInWhen.kt"); // Commented
        excludedFiles.add("kt1592.kt"); // Codegen don't execute blackBoxFile() on it

        excludedFiles.add("box.kt");      // MultiFileTest not supported yet
        excludedFiles.add("kt2060_1.kt"); // MultiFileTest not supported yet
        excludedFiles.add("kt2257_1.kt"); // MultiFileTest not supported yet
        excludedFiles.add("kt1528_1.kt"); // MultiFileTest not supported yet
        excludedFiles.add("thisPackage.kt"); // MultiFileTest not supported yet

        excludedFiles.add("kt684.kt"); // StackOverflow with StringBuilder (escape())

        excludedFiles.add("kt344.kt"); // Bug KT-2251
        excludedFiles.add("kt529.kt");  // Bug

        excludedFiles.add("noClassObjectForJavaClass.kt");

        excludedFiles.add("doGenerateAssertions.kt"); // Multi-file + Java
        excludedFiles.add("doNotGenerateAssertions.kt"); // Multi-file + Java
        excludedFiles.add("doGenerateParamAssertions.kt"); // Java

        excludedFiles.add("nestedInPackage.kt"); // Custom packages are not supported
        excludedFiles.add("importNestedClass.kt"); // Won't work when moved to another package

        excludedFiles.add("protectedStaticClass.kt");                       // Java
        excludedFiles.add("protectedStaticClass2.kt");                      // Java
        excludedFiles.add("protectedStaticFun.kt");                         // Java
        excludedFiles.add("protectedStaticFunCallInConstructor.kt");        // Java
        excludedFiles.add("protectedStaticFunClassObject.kt");              // Java
        excludedFiles.add("protectedStaticFunGenericClass.kt");             // Java
        excludedFiles.add("protectedStaticFunNestedStaticClass.kt");        // Java
        excludedFiles.add("protectedStaticFunNestedStaticClass2.kt");       // Java
        excludedFiles.add("protectedStaticFunNestedStaticGenericClass.kt"); // Java
        excludedFiles.add("protectedStaticFunNotDirectSuperClass.kt");      // Java
        excludedFiles.add("protectedStaticFunObject.kt");                   // Java
        excludedFiles.add("protectedStaticProperty.kt");                    // Java

    }

    private SpecialFiles() {
    }
}
