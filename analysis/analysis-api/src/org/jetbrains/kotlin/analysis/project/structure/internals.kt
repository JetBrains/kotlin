/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.analysis.project.structure

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependencies
import org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependenciesOfType
import org.jetbrains.kotlin.analysis.api.projectStructure.danglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.directFriendDependenciesOfType
import org.jetbrains.kotlin.analysis.api.projectStructure.directRegularDependenciesOfType
import org.jetbrains.kotlin.analysis.api.projectStructure.isDangling
import org.jetbrains.kotlin.psi.KtFile

// These callables are needed as a bridge between `*.analysis.project.structure.callableName` and
// `*.analysis.api.projectStructure.callableName`, as we cannot access extension properties/functions with a fully qualified name.

internal fun isDanglingFile(file: KtFile): Boolean = file.isDangling

internal fun getDanglingFileResolutionMode(file: KtFile): KaDanglingFileResolutionMode? = file.danglingFileResolutionMode

@KaImplementationDetail
public inline fun <reified M : KaModule> KaModule.getDirectRegularDependenciesOfType(): Sequence<M> = directRegularDependenciesOfType()

@KaImplementationDetail
public inline fun <reified M : KaModule> KaModule.getDirectFriendDependenciesOfType(): Sequence<M> = directFriendDependenciesOfType()

@KaImplementationDetail
public inline fun <reified M : KaModule> KaModule.getDirectDependsOnDependenciesOfType(): Sequence<M> = directRegularDependenciesOfType()

internal fun KaModule.getAllDirectDependencies(): Sequence<KaModule> = allDirectDependencies()

@KaImplementationDetail
public inline fun <reified M : KaModule> KaModule.getAllDirectDependenciesOfType(): Sequence<M> = allDirectDependenciesOfType()
