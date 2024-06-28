/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

/**
 * [NoMutableState] annotation means that annotated class has no mutable state
 *   and it's safe to use it concurrent environment (e.g. as session component)
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class NoMutableState

/**
 * [ThreadSafeMutableState] annotation means that annotated class has mutable state
 *   and it should carefully implement it for concurrent environment if it will be used
 *   as session component
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ThreadSafeMutableState

/**
 * All [FirSession]s should be created via FirSessionFactories, so constructors of all [FirSession] inheritors
 * should be marked with this annotation, to avoid accidental creating session without factory
 */
@RequiresOptIn
annotation class PrivateSessionConstructor

/**
 * [SessionConfiguration] is used to avoid accidental registration of session components after
 *   session was initialized
 */
@RequiresOptIn
annotation class SessionConfiguration

