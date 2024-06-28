/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.projectStructure

/**
 * A list of all modules that the current module can depend on with regular dependency.
 *
 * @see KaModule.directRegularDependencies
 */
public inline fun <reified M : KaModule> KaModule.directRegularDependenciesOfType(): Sequence<M> =
    directRegularDependencies.asSequence().filterIsInstance<M>()

/**
 * A list of all modules that the current module can depend on with friend dependency.
 *
 * @see KaModule.directFriendDependencies
 */
public inline fun <reified M : KaModule> KaModule.directFriendDependenciesOfType(): Sequence<M> =
    directFriendDependencies.asSequence().filterIsInstance<M>()

/**
 * A list of all modules that the current module can depend on with refinement dependency.
 *
 * @see KaModule.directDependsOnDependencies
 */
public inline fun <reified M : KaModule> KaModule.directDependsOnDependenciesOfType(): Sequence<M> =
    directDependsOnDependencies.asSequence().filterIsInstance<M>()

/**
 * A list of all other modules that the current module can depend on.
 *
 * @see KaModule.directRegularDependencies
 * @see KaModule.directDependsOnDependencies
 * @see KaModule.directFriendDependencies
 */
public fun KaModule.allDirectDependencies(): Sequence<KaModule> =
    sequence {
        yieldAll(directRegularDependencies)
        yieldAll(directDependsOnDependencies)
        yieldAll(directFriendDependencies)
    }

/**
 * A list of all other modules of type [M] that the current module can depend on.
 *
 * @see KaModule.directRegularDependencies
 * @see KaModule.directDependsOnDependencies
 * @see KaModule.directFriendDependencies
 */
public inline fun <reified M : KaModule> KaModule.allDirectDependenciesOfType(): Sequence<M> =
    allDirectDependencies().filterIsInstance<M>()
