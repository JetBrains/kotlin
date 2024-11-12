/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.analysis.project.structure

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

@OptIn(KaImplementationDetail::class)
@Deprecated("Use 'org.jetbrains.kotlin.analysis.api.projectStructure.directRegularDependenciesOfType' instead.")
public inline fun <reified M : KaModule> KaModule.directRegularDependenciesOfType(): Sequence<M> = getDirectRegularDependenciesOfType()

@OptIn(KaImplementationDetail::class)
@Deprecated("Use 'org.jetbrains.kotlin.analysis.api.projectStructure.directFriendDependenciesOfType' instead.")
public inline fun <reified M : KaModule> KaModule.directFriendDependenciesOfType(): Sequence<M> = getDirectFriendDependenciesOfType()

@OptIn(KaImplementationDetail::class)
@Deprecated("Use 'org.jetbrains.kotlin.analysis.api.projectStructure.directDependsOnDependenciesOfType' instead.")
public inline fun <reified M : KaModule> KaModule.directDependsOnDependenciesOfType(): Sequence<M> = getDirectDependsOnDependenciesOfType()

@Deprecated("Use 'org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependencies' instead.")
public fun KaModule.allDirectDependencies(): Sequence<KaModule> = getAllDirectDependencies()

@OptIn(KaImplementationDetail::class)
@Deprecated("Use 'org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependenciesOfType' instead.")
public inline fun <reified M : KaModule> KaModule.allDirectDependenciesOfType(): Sequence<M> = getAllDirectDependenciesOfType()
