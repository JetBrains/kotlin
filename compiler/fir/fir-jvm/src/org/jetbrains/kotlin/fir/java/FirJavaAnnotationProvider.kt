/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.load.java.JavaAnnotationProvider

interface FirJavaAnnotationProvider : JavaAnnotationProvider, FirSessionComponent

val FirSession.javaAnnotationProvider: FirJavaAnnotationProvider? by FirSession.nullableSessionComponentAccessor()