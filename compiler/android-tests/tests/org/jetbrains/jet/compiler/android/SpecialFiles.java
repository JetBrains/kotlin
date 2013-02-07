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
        excludedFiles.add("kt326.kt"); // Commented
        excludedFiles.add("kt1213.kt"); // Commented

        excludedFiles.add("box.kt");      // MultiFileTest not supported yet
        excludedFiles.add("kt2060_1.kt"); // MultiFileTest not supported yet
        excludedFiles.add("kt2257_1.kt"); // MultiFileTest not supported yet
        excludedFiles.add("kt1528_1.kt"); // MultiFileTest not supported yet
        excludedFiles.add("kt1845_1.kt"); // MultiFileTest not supported yet
        excludedFiles.add("thisPackage.kt"); // MultiFileTest not supported yet

        excludedFiles.add("kt684.kt"); // StackOverflow with StringBuilder (escape())

        excludedFiles.add("kt529.kt");  // Bug
        excludedFiles.add("kt344.kt");  // Bug

        excludedFiles.add("classWithNestedEnum.kt");

        excludedFiles.add("doGenerateAssertions.kt"); // Multi-file + Java
        excludedFiles.add("doNotGenerateAssertions.kt"); // Multi-file + Java
        excludedFiles.add("doGenerateParamAssertions.kt"); // Java

        excludedFiles.add("nestedInPackage.kt"); // Custom packages are not supported
        excludedFiles.add("importNestedClass.kt"); // Won't work when moved to another package

        excludedFiles.add("funCallInConstructor.kt");        // Java
        excludedFiles.add("funClassObject.kt");              // Java
        excludedFiles.add("funGenericClass.kt");             // Java
        excludedFiles.add("funNestedStaticClass.kt");        // Java
        excludedFiles.add("funNestedStaticClass2.kt");       // Java
        excludedFiles.add("funNestedStaticGenericClass.kt"); // Java
        excludedFiles.add("funNotDirectSuperClass.kt");      // Java
        excludedFiles.add("funObject.kt");                   // Java
        excludedFiles.add("simpleClass.kt");                 // Java
        excludedFiles.add("simpleClass2.kt");                // Java
        excludedFiles.add("simpleFun.kt");                   // Java
        excludedFiles.add("simpleProperty.kt");              // Java

        excludedFiles.add("packageClass.kt");                    // Java
        excludedFiles.add("packageFun.kt");                      // Java
        excludedFiles.add("packageProperty.kt");                 // Java
        excludedFiles.add("overrideProtectedFunInPackage.kt");   // Java
        excludedFiles.add("protectedFunInPackage.kt");           // Java
        excludedFiles.add("protectedPropertyInPackage.kt");      // Java
        excludedFiles.add("protectedStaticClass.kt");            // Java
    }

    private SpecialFiles() {
    }
}
