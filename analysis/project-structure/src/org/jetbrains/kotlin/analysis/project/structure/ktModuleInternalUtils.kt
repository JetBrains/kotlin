/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure

import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.psi.UserDataProperty

/**
 * Used by the [org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionProvider] implementations
 * to store the references on the [KtModule] for which the [VirtualFile] was generated.
 */
@KtModuleStructureInternals
public var VirtualFile.analysisExtensionFileContextModule: KtModule? by UserDataProperty(Key.create("ANALYSIS_CONTEXT_MODULE"))

@RequiresOptIn("Internal KtModule structure API component which should not be used outside the Analysis API implementation modules as it does not have any compatibility guarantees")
public annotation class KtModuleStructureInternals
