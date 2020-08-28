/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.registerJvmEffectiveVisibilityResolver
import org.jetbrains.kotlin.fir.resolve.calls.jvm.registerJvmCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.registerJavaClassMapper
import org.jetbrains.kotlin.fir.resolve.registerJavaSyntheticNamesProvider

fun FirSession.registerJavaSpecificComponents() {
    registerJavaVisibilityChecker()
    registerJvmCallConflictResolverFactory()
    registerJvmEffectiveVisibilityResolver()
    registerJavaClassMapper()
    registerJavaSyntheticNamesProvider()
}
