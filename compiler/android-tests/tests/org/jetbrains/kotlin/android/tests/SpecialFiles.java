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

        excludedFiles.add("enclosing"); //reflection tests
        excludedFiles.add("noReflectAtRuntime"); //reflection tests
        excludedFiles.add("methodsFromAny"); //reflection tests
        excludedFiles.add("genericProperty.kt"); //reflection tests

        excludedFiles.add("kt3238.kt"); // Reflection
        excludedFiles.add("kt1482_2279.kt"); // Reflection
        excludedFiles.add("extensionMethod.kt"); // Reflection loadClass

        excludedFiles.add("nestedInPackage.kt"); // Cannot change package name
        excludedFiles.add("packageQualifiedMethod.kt"); // Cannot change package name
        excludedFiles.add("classObjectToString.kt"); // Cannot change package name
        excludedFiles.add("assertionStackTrace.kt"); // Cannot change package name
        excludedFiles.add("anonymousObjectReifiedSupertype.kt"); // Cannot change package name
        excludedFiles.add("innerAnonymousObject.kt"); // Cannot change package name
        excludedFiles.add("nestedReifiedSignature.kt"); // Cannot change package name
        excludedFiles.add("recursiveInnerAnonymousObject.kt"); // Cannot change package name
        excludedFiles.add("approximateCapturedTypes.kt"); // Cannot change package name
        excludedFiles.add("classForEnumEntry.kt"); // Cannot change package name
        excludedFiles.add("kt10143.kt"); // Cannot change package name
        excludedFiles.add("internalTopLevelOtherPackage.kt"); // Cannot change package name
        excludedFiles.add("noPrivateDelegation.kt"); // Cannot change package name
        excludedFiles.add("platformTypeAssertionStackTrace.kt"); // Cannot change package name
        excludedFiles.add("packages.kt"); // Cannot change package name
        excludedFiles.add("kt10259.kt"); // Cannot change package name
        excludedFiles.add("kt11081.kt"); // Cannot change package name
        excludedFiles.add("kt6990.kt"); // Cannot change package name
        excludedFiles.add("mainInFiles.kt"); // Cannot change package name
        excludedFiles.add("noClassForSimpleEnum.kt"); // Cannot change package name
        excludedFiles.add("simpleClassLiteral.kt"); // Cannot change package name
        excludedFiles.add("jvmName.kt"); // Cannot change package name
        excludedFiles.add("qualifiedName.kt"); // Cannot change package name
        excludedFiles.add("topLevelProperty.kt"); // Cannot change package name
        excludedFiles.add("typeParameters.kt"); // Cannot change package name

        excludedFiles.add("kt684.kt"); // StackOverflow with StringBuilder (escape())

        excludedFiles.add("genericBackingFieldSignature.kt"); // Wrong signature after package renaming
        excludedFiles.add("genericMethodSignature.kt"); // Wrong signature after package renaming
        excludedFiles.add("kt11121.kt"); // Wrong signature after package renaming
        excludedFiles.add("kt5112.kt"); // Wrong signature after package renaming

        excludedFiles.add("classpath.kt"); // Some classes are not visible on android

        excludedFiles.add("manyNumbers.kt"); // Out of memory

        excludedFiles.add("external"); //native methods

        excludedFiles.add("enclosingInfo"); //  Wrong enclosing info after package renaming
        excludedFiles.add("signature"); //  Wrong signature after package renaming

        excludedFiles.add("functionNtoStringNoReflect.kt"); // disabled cause test executed with reflection

        excludedFiles.add("nestedClasses.kt"); // additional nested class in 'Thread' class on Android
        excludedFiles.add("kt12200Const.kt"); // no 'modifiers' field in 'java.lang.reflect.Field' class

        excludedFiles.add("closureOfInnerLocalClass.kt"); // KT-8120
        excludedFiles.add("closureWithSelfInstantiation.kt"); // KT-8120
    }

    private SpecialFiles() {
    }
}
