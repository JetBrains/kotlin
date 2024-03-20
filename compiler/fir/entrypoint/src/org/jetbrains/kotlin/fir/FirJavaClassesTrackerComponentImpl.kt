/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.java.FirJavaClassesTrackerComponent
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.incremental.components.ModuleJavaClassesTracker
import java.io.File

class FirJavaClassesTrackerComponentImpl(private val javaClassesTracker: ModuleJavaClassesTracker) : FirJavaClassesTrackerComponent() {

    override fun report(javaClass: FirJavaClass, file: FirFile?) {
        javaClassesTracker.report(javaClass.classId, file?.sourceFile?.path?.let(::File))
    }
}
