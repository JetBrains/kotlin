/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure

/**
 * A list of all modules that the current module can depend on with regular dependency.
 *
 * @see KtModule.directRegularDependencies
 */
public inline fun <reified M : KtModule> KtModule.directRegularDependenciesOfType(): Sequence<M> =
    directRegularDependencies.asSequence().filterIsInstance<M>()

/**
 * A list of all modules that the current module can depend on with friend dependency.
 *
 * @see KtModule.directFriendDependencies
 */
public inline fun <reified M : KtModule> KtModule.directFriendDependenciesOfType(): Sequence<M> =
    directFriendDependencies.asSequence().filterIsInstance<M>()

/**
 * A list of all modules that the current module can depend on with refinement dependency.
 *
 * @see KtModule.directDependsOnDependencies
 */
public inline fun <reified M : KtModule> KtModule.directDependsOnDependenciesOfType(): Sequence<M> =
    directDependsOnDependencies.asSequence().filterIsInstance<M>()

/**
 * A list of all other modules that the current module can depend on.
 *
 * @see KtModule.directRegularDependencies
 * @see KtModule.directDependsOnDependencies
 * @see KtModule.directFriendDependencies
 */
public fun KtModule.allDirectDependencies(): Sequence<KtModule> =
    sequence {
        yieldAll(directRegularDependencies)
        yieldAll(directDependsOnDependencies)
        yieldAll(directFriendDependencies)
    }

/**
 * A list of all other modules of type [M] that the current module can depend on.
 *
 * @see KtModule.directRegularDependencies
 * @see KtModule.directDependsOnDependencies
 * @see KtModule.directFriendDependencies
 */
public inline fun <reified M : KtModule> KtModule.allDirectDependenciesOfType(): Sequence<M> =
    allDirectDependencies().filterIsInstance<M>()