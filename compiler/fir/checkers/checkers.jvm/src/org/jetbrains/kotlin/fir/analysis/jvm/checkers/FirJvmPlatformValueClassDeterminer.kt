/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.config.JDK_VALUE_CLASSES
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.isJvmTargetValhallaCompatible
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.analysis.checkers.FirPlatformValueClassDeterminer
import org.jetbrains.kotlin.fir.java.jvmTargetProvider
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

/**
 * The JDK classes of [JDK_VALUE_CLASSES] are value classes with the JVM preview features of a Valhalla-compatible target. The JDK's
 * non-preview class files, which are the ones read, declare them as identity classes.
 */
@NoMutableState
class FirJvmPlatformValueClassDeterminer(private val session: FirSession) : FirPlatformValueClassDeterminer() {
    override fun isPlatformValueClass(symbol: FirRegularClassSymbol): Boolean {
        val jvmTarget = session.jvmTargetProvider?.jvmTarget
        val isJvmPreviewEnabled = session.languageVersionSettings.getFlag(JvmAnalysisFlags.enableJvmPreview)
        val hasValueClasses = jvmTarget?.let { isJvmTargetValhallaCompatible(it, isJvmPreviewEnabled) } == true
        return hasValueClasses && JvmClassName.internalNameByClassId(symbol.classId) in JDK_VALUE_CLASSES
    }
}
