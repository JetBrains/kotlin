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

package org.jetbrains.kotlin.android.tests;

import com.google.common.collect.Sets;

import java.util.Set;

public class SpecialFiles {
    private static final Set<String> excludedFiles = Sets.newHashSet();
    private static final Set<String> filesCompiledWithoutStdLib = Sets.newHashSet();

    static {
        fillExcludedFiles();
    }


    public static Set<String> getExcludedFiles() {
        return excludedFiles;
    }

    private static void fillExcludedFiles() {
        excludedFiles.add("native"); // Reflection is used to check full class name

        excludedFiles.add("reflection");
        excludedFiles.add("kt3238.kt"); // Reflection
        excludedFiles.add("kt1482_2279.kt"); // Reflection

        excludedFiles.add("nestedInPackage.kt"); // Cannot change package name
        excludedFiles.add("importNestedClass.kt"); // Cannot change package name
        excludedFiles.add("packageQualifiedMethod.kt"); // Cannot change package name
        excludedFiles.add("classObjectToString.kt"); // Cannot change package name
        excludedFiles.add("invokeOnClassObjectOfNestedClass2.kt"); // Cannot change package name
        excludedFiles.add("invokeOnImportedEnum1.kt"); // Cannot change package name
        excludedFiles.add("invokeOnImportedEnum2.kt"); // Cannot change package name
        excludedFiles.add("sortEnumEntries.kt"); // Cannot change package name
        excludedFiles.add("assertionStackTrace.kt"); // Cannot change package name
        excludedFiles.add("anonymousObjectReifiedSupertype.kt"); // Cannot change package name
        excludedFiles.add("innerAnonymousObject.kt"); // Cannot change package name
        excludedFiles.add("nestedReifiedSignature.kt"); // Cannot change package name
        excludedFiles.add("recursiveInnerAnonymousObject.kt"); // Cannot change package name
        excludedFiles.add("approximateCapturedTypes.kt"); // Cannot change package name
        excludedFiles.add("classForEnumEntry.kt"); // Cannot change package name

        excludedFiles.add("kt684.kt"); // StackOverflow with StringBuilder (escape())

        excludedFiles.add("kt529.kt");  // Bug
        excludedFiles.add("kt344.kt");  // Bug

        excludedFiles.add("comparisonWithNullCallsFun.kt"); // java.lang.NoClassDefFoundError: kotlin.Nothing
        excludedFiles.add("kt3574.kt"); // java.lang.NoClassDefFoundError: kotlin.Nothing

        excludedFiles.add("genericBackingFieldSignature.kt"); // Wrong signature after package renaming
        excludedFiles.add("genericMethodSignature.kt"); // Wrong signature after package renaming

        excludedFiles.add("classpath.kt"); // Some classes are not visible on android

        excludedFiles.add("manyNumbers.kt"); // Out of memory

        excludedFiles.add("smap"); // Line numbers

        // TODO: fix import processing
        excludedFiles.add("useImportedMemberFromCompanion.kt");
        excludedFiles.add("useImportedMember.kt");
        excludedFiles.add("importStaticMemberFromObject.kt");
        //TODO: fix KT-12127
        excludedFiles.add("genericProperty.kt");

        excludedFiles.add("external"); //native methods
    }

    private SpecialFiles() {
    }
}
