/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.syntax.SourcesToPathsMapper

/**
 * Holds the [SourcesToPathsMapper] of a session, which is created for the parse tree of its sources.
 */
class FirSourcesToPathsMapperHolder(internal val mapper: SourcesToPathsMapper) : FirSessionComponent

private val FirSession.sourcesToPathsMapperHolder: FirSourcesToPathsMapperHolder by FirSession.sessionComponentAccessor()

val FirSession.sourcesToPathsMapper: SourcesToPathsMapper
    get() = sourcesToPathsMapperHolder.mapper
