/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.psi.PsiElement

interface DiagnosticMarker {
    val psiElement: PsiElement
    val factoryName: String
}

interface DiagnosticWithParameters1Marker<A> : DiagnosticMarker {
    val a: A
}

interface DiagnosticWithParameters2Marker<A, B> : DiagnosticMarker {
    val a: A
    val b: B
}

interface DiagnosticWithParameters3Marker<A, B, C> : DiagnosticMarker {
    val a: A
    val b: B
    val c: C
}

interface DiagnosticWithParameters4Marker<A, B, C, D> : DiagnosticMarker {
    val a: A
    val b: B
    val c: C
    val d: D
}
