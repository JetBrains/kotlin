/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

    static {
        fillExcludedFiles();
    }

    public static Set<String> getExcludedFiles() {
        return excludedFiles;
    }

    private static void fillExcludedFiles() {
        // Reflection
        excludedFiles.add("enclosing");
        excludedFiles.add("noReflectAtRuntime");
        excludedFiles.add("methodsFromAny");
        excludedFiles.add("genericProperty.kt");
        excludedFiles.add("kt3238.kt");
        excludedFiles.add("kt1482_2279.kt");
        excludedFiles.add("extensionMethod.kt");
        excludedFiles.add("functionNtoStringNoReflect.kt");
        excludedFiles.add("innerGeneric.kt");
        excludedFiles.add("simpleCreateType.kt");
        excludedFiles.add("equalsHashCodeToString.kt");
        excludedFiles.add("arrayOfKClasses.kt");
        excludedFiles.add("enumKClassAnnotation.kt");
        excludedFiles.add("primitivesAndArrays.kt");
        excludedFiles.add("getDelegateWithoutReflection.kt");

        // Reflection is used to check full class name
        excludedFiles.add("native");

        // "IOOBE: Invalid index 4, size is 4" for java.lang.reflect.ParameterizedType on Android
        excludedFiles.add("innerGenericTypeArgument.kt");

        // Cannot change package name
        excludedFiles.add("nestedInPackage.kt");
        excludedFiles.add("packageQualifiedMethod.kt");
        excludedFiles.add("classObjectToString.kt");
        excludedFiles.add("assertionStackTrace.kt");
        excludedFiles.add("anonymousObjectReifiedSupertype.kt");
        excludedFiles.add("innerAnonymousObject.kt");
        excludedFiles.add("nestedReifiedSignature.kt");
        excludedFiles.add("recursiveInnerAnonymousObject.kt");
        excludedFiles.add("approximateCapturedTypes.kt");
        excludedFiles.add("classForEnumEntry.kt");
        excludedFiles.add("kt10143.kt");
        excludedFiles.add("internalTopLevelOtherPackage.kt");
        excludedFiles.add("noPrivateDelegation.kt");
        excludedFiles.add("platformTypeAssertionStackTrace.kt");
        excludedFiles.add("packages.kt");
        excludedFiles.add("kt10259.kt");
        excludedFiles.add("kt11081.kt");
        excludedFiles.add("kt6990.kt");
        excludedFiles.add("mainInFiles.kt");
        excludedFiles.add("noClassForSimpleEnum.kt");
        excludedFiles.add("simpleClassLiteral.kt");
        excludedFiles.add("jvmName.kt");
        excludedFiles.add("qualifiedName.kt");
        excludedFiles.add("topLevelProperty.kt");
        excludedFiles.add("typeParameters.kt");
        excludedFiles.add("kt13133.kt");
        excludedFiles.add("genericOverriddenFunction.kt");
        excludedFiles.add("genericOverriddenProperty.kt");
        excludedFiles.add("genericProperty.kt");

        // StackOverflow with StringBuilder (escape())
        excludedFiles.add("kt684.kt");

        // Wrong enclosing info or signature after package renaming
        excludedFiles.add("enclosingInfo");
        excludedFiles.add("signature");
        excludedFiles.add("genericBackingFieldSignature.kt");
        excludedFiles.add("genericMethodSignature.kt");
        excludedFiles.add("kt11121.kt");
        excludedFiles.add("kt5112.kt");

        // Different format of inner signature on Android and JVM
        excludedFiles.add("signatureOfDeepGenericInner.kt");
        excludedFiles.add("signatureOfDeepInner.kt");
        excludedFiles.add("signatureOfDeepInnerLastGeneric.kt");
        excludedFiles.add("signatureOfGenericInnerGenericOuter.kt");
        excludedFiles.add("signatureOfGenericInnerSimpleOuter.kt");
        excludedFiles.add("signatureOfSimpleInnerSimpleOuter.kt");

        // Some classes are not visible on android
        excludedFiles.add("classpath.kt");

        // Out of memory
        excludedFiles.add("manyNumbers.kt");

        // Native methods
        excludedFiles.add("external");

        // Additional nested class in 'Thread' class on Android
        excludedFiles.add("nestedClasses.kt");
        // No 'modifiers' field in 'java.lang.reflect.Field' class
        excludedFiles.add("kt12200Const.kt");

        // KT-8120
        excludedFiles.add("closureOfInnerLocalClass.kt");
        excludedFiles.add("closureWithSelfInstantiation.kt");
        excludedFiles.add("quotedClassName.kt");
    }

    private SpecialFiles() {
    }
}
