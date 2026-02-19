/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.jvm

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.replSnippetResolveExtensions
import org.jetbrains.kotlin.fir.resolve.calls.overloads.ConeCallConflictResolver
import org.jetbrains.kotlin.fir.resolve.calls.overloads.ConeCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.calls.overloads.ReplOverloadCallConflictResolver

@NoMutableState
object JvmCallConflictResolverFactory : ConeCallConflictResolverFactory() {
    override fun createAdditionalResolvers(session: FirSession): List<ConeCallConflictResolver> {
        val isRepl = session.extensionService.replSnippetResolveExtensions.isNotEmpty()
        return buildList {
            if (isRepl) add(ReplOverloadCallConflictResolver)
            add(JvmPlatformOverloadsConflictResolver(session))
        }
    }
}
