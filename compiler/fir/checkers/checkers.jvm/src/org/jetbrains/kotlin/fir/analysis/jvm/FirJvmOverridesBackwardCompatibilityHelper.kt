/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.analysis.FirOverridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.isJavaOrEnhancement

/**
 * With JVM compilation
 * `override` keyword can be omitted if the super member is declared in a Java class that has a special Kotlin class mapping.
 * For example, overriding members of
 *    `kotlin.Throwable` does not require `override` keyword.
 */
object FirJvmOverridesBackwardCompatibilityHelper : FirOverridesBackwardCompatibilityHelper() {
    override fun additionalCheck(member: FirCallableDeclaration): Boolean? {
        if (!member.isJavaOrEnhancement) return false
        val containingClassName = member.containingClassLookupTag()?.classId?.asSingleFqName()?.toUnsafe() ?: return false
        // If the super class is mapped to a Kotlin built-in class, then we don't require `override` keyword.
        if (JavaToKotlinClassMap.mapKotlinToJava(containingClassName) != null) {
            return true
        }
        return null
    }
}
