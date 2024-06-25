/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.analysis.project.structure

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail

@OptIn(KaImplementationDetail::class)
@Deprecated("Use 'org.jetbrains.kotlin.analysis.api.projectStructure.directRegularDependenciesOfType' instead.")
public inline fun <reified M : KtModule> KtModule.directRegularDependenciesOfType(): Sequence<M> = getDirectRegularDependenciesOfType()

@OptIn(KaImplementationDetail::class)
@Deprecated("Use 'org.jetbrains.kotlin.analysis.api.projectStructure.directFriendDependenciesOfType' instead.")
public inline fun <reified M : KtModule> KtModule.directFriendDependenciesOfType(): Sequence<M> = getDirectFriendDependenciesOfType()

@OptIn(KaImplementationDetail::class)
@Deprecated("Use 'org.jetbrains.kotlin.analysis.api.projectStructure.directDependsOnDependenciesOfType' instead.")
public inline fun <reified M : KtModule> KtModule.directDependsOnDependenciesOfType(): Sequence<M> = getDirectDependsOnDependenciesOfType()

@Deprecated("Use 'org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependencies' instead.")
public fun KtModule.allDirectDependencies(): Sequence<KtModule> = getAllDirectDependencies()

@OptIn(KaImplementationDetail::class)
@Deprecated("Use 'org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependenciesOfType' instead.")
public inline fun <reified M : KtModule> KtModule.allDirectDependenciesOfType(): Sequence<M> = getAllDirectDependenciesOfType()
