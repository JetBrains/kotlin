/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.config.JDK_VALUE_CLASSES
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.isJvmTargetValhallaCompatible
import org.jetbrains.kotlin.fir.FirPlatformValueClassDeterminer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.isJavaValueClass
import org.jetbrains.kotlin.fir.java.jvmTargetProvider
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

/**
 * Java value classes are value classes, and so are the JDK classes of [JDK_VALUE_CLASSES] with the JVM preview features of a
 * Valhalla-compatible target, although the JDK's non-preview class files, which are the ones read, declare them as identity classes.
 * The JDK classes annotated with `@jdk.internal.ValueBased` are value-based (JEP 390).
 */
@NoMutableState
class FirJvmPlatformValueClassDeterminer(private val session: FirSession) : FirPlatformValueClassDeterminer() {
    override fun isPlatformValueClass(symbol: FirRegularClassSymbol): Boolean =
        symbol.isJavaValueClass ||
                session.isJvmTargetValhallaCompatible && JvmClassName.internalNameByClassId(symbol.classId) in JDK_VALUE_CLASSES

    override fun isValueBasedClass(symbol: FirRegularClassSymbol): Boolean =
        symbol.hasAnnotation(JvmStandardClassIds.Annotations.ValueBased, session)
}

// Whether the JVM target of this compilation has value classes (JEP 401).
internal val FirSession.isJvmTargetValhallaCompatible: Boolean
    get() {
        val jvmTarget = jvmTargetProvider?.jvmTarget ?: return false
        return isJvmTargetValhallaCompatible(jvmTarget, languageVersionSettings.getFlag(JvmAnalysisFlags.enableJvmPreview))
    }
