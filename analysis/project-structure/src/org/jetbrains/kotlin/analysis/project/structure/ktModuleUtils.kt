/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure

/**
 * A list of all modules current module can depend on with regular dependency
 *
 * @see KtModule.directRegularDependencies
 */
public inline fun <reified M : KtModule> KtModule.directRegularDependenciesOfType(): Sequence<M> =
    directRegularDependencies.asSequence().filterIsInstance<M>()

/**
 * A list of all modules current module can depend on with friend dependency
 *
 * @see KtModule.directFriendDependencies
 */
public inline fun <reified M : KtModule> KtModule.directFriendDependenciesOfType(): Sequence<M> =
    directFriendDependencies.asSequence().filterIsInstance<M>()

/**
 * A list of all modules current module can depend on with refinement dependency
 *
 * @see KtModule.directRefinementDependencies
 */
public inline fun <reified M : KtModule> KtModule.directRefinementDependenciesOfType(): Sequence<M> =
    directRefinementDependencies.asSequence().filterIsInstance<M>()



/**
 * A list of all other modules current module can depend on.
 *
 * @see KtModule.directRegularDependencies
 * @see KtModule.directRefinementDependencies
 * @see KtModule.directFriendDependencies
 */
public fun KtModule.allDirectDependencies(): Sequence<KtModule> =
    sequence {
        yieldAll(directRegularDependencies)
        yieldAll(directRefinementDependencies)
        yieldAll(directFriendDependencies)
    }

/**
 * A list of all other modules of type [M] current module can depend on.
 *
 * @see KtModule.directRegularDependencies
 * @see KtModule.directRefinementDependencies
 * @see KtModule.directFriendDependencies
 */
public inline fun <reified M : KtModule> KtModule.allDirectDependenciesOfType(): Sequence<M> =
    allDirectDependencies().filterIsInstance<M>()