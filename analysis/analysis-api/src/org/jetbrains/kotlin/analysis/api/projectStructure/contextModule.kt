/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.projectStructure

import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.psi.UserDataProperty

/**
 * When applied to a virtual file *A*, [analysisContextModule] determines the context module of any [KaDanglingFileModule] whose file has
 * *A* as an *original file*. It does not affect the [KaModule] of *A* itself.
 */
@Deprecated("`analysisContextModule` has problematic semantics (see KT-80856). Consider `KtFile.contextModule` instead.")
@KaImplementationDetail
public var VirtualFile.analysisContextModule: KaModule? by UserDataProperty(Key.create("ANALYSIS_CONTEXT_MODULE"))
