/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.builder

import com.intellij.openapi.project.Project
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub

fun stubComputationTrackerInstance(project: Project): StubComputationTracker? {
    return project.getComponent(StubComputationTracker::class.java)
}

interface StubComputationTracker {
    fun onStubComputed(javaFileStub: PsiJavaFileStub, context: LightClassConstructionContext)
}
