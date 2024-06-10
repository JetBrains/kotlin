/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure

import org.jetbrains.kotlin.analysis.api.projectStructure.danglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.isDangling
import org.jetbrains.kotlin.psi.KtFile

// These properties are needed as a bridge between `*.analysis.project.structure.propertyName` and
// `*.analysis.api.projectStructure.propertyName`, as we cannot access extension properties with a fully qualified name.

internal fun isDanglingFile(file: KtFile): Boolean = file.isDangling

internal fun getDanglingFileResolutionMode(file: KtFile): DanglingFileResolutionMode? = file.danglingFileResolutionMode
