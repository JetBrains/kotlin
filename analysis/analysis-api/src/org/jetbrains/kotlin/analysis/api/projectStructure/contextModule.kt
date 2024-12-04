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
 * Used by [KtResolveExtensionProvider][org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionProvider] implementations to
 * store the references on the [KaModule] for which the [VirtualFile] was generated.
 *
 * Used code fragments in the debuggers evaluate expression to set the context module of [KaDanglingFileModule]s.
 * This is particularly useful in KMP where the 'contextElement' of a code block might be located in a commonModule, however
 * the context e.g. should be a leaf JVM module.
 */
@KaImplementationDetail
public var VirtualFile.analysisContextModule: KaModule? by UserDataProperty(Key.create("ANALYSIS_CONTEXT_MODULE"))
