/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure

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

public typealias KtModule = KaModule

public typealias KtSourceModule = KaSourceModule

public typealias KtBinaryModule = KaBinaryModule

public typealias KtLibraryModule = KaLibraryModule

public typealias KtSdkModule = KaSdkModule

public typealias KtLibrarySourceModule = KaLibrarySourceModule

public typealias KtBuiltinsModule = KaBuiltinsModule

public typealias KtScriptModule = KaScriptModule

public typealias KtScriptDependencyModule = KaScriptDependencyModule

public typealias KtDanglingFileModule = KaDanglingFileModule

public typealias KtNotUnderContentRootModule = KaNotUnderContentRootModule
