/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBinaryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptDependencyModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSdkModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule

@Deprecated("Use 'KaModule' instead", ReplaceWith("KaModule"))
public typealias KtModule = KaModule

@Deprecated("Use 'KaSourceModule' instead", ReplaceWith("KaSourceModule"))
public typealias KtSourceModule = KaSourceModule

@Deprecated("Use 'KaBinaryModule' instead", ReplaceWith("KaBinaryModule"))
public typealias KtBinaryModule = KaBinaryModule

@Deprecated("Use 'KaLibraryModule' instead", ReplaceWith("KaLibraryModule"))
public typealias KtLibraryModule = KaLibraryModule

@Deprecated("Use 'KaSdkModule' instead", ReplaceWith("KaSdkModule"))
public typealias KtSdkModule = KaSdkModule

@Deprecated("Use 'KaLibrarySourceModule' instead", ReplaceWith("KaLibrarySourceModule"))
public typealias KtLibrarySourceModule = KaLibrarySourceModule

@Deprecated("Use 'KaBuiltinsModule' instead", ReplaceWith("KaBuiltinsModule"))
@KaPlatformInterface
public typealias KtBuiltinsModule = KaBuiltinsModule

@Deprecated("Use 'KaScriptModule' instead", ReplaceWith("KaScriptModule"))
@KaExperimentalApi
public typealias KtScriptModule = KaScriptModule

@Deprecated("Use 'KaScriptDependencyModule' instead", ReplaceWith("KaScriptDependencyModule"))
@KaPlatformInterface
public typealias KtScriptDependencyModule = KaScriptDependencyModule

@Deprecated("Use 'KaDanglingFileModule' instead", ReplaceWith("KaDanglingFileModule"))
@KaPlatformInterface
public typealias KtDanglingFileModule = KaDanglingFileModule

@Deprecated("Use 'KaNotUnderContentRootModule' instead", ReplaceWith("KaNotUnderContentRootModule"))
@KaPlatformInterface
public typealias KtNotUnderContentRootModule = KaNotUnderContentRootModule
