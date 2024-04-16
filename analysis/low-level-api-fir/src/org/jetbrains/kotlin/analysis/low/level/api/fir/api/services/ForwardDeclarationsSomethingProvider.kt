/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.services

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.FirSession

abstract class ForwardDeclarationsSomethingProvider {
    abstract fun getForwardDeclarationsScope(session: FirSession): GlobalSearchScope

    companion object {
        fun getInstance(project: Project): ForwardDeclarationsSomethingProvider =
            project.getService(ForwardDeclarationsSomethingProvider::class.java) ?: Default

        val Default: ForwardDeclarationsSomethingProvider = object : ForwardDeclarationsSomethingProvider() {
            override fun getForwardDeclarationsScope(session: FirSession): GlobalSearchScope {
                return GlobalSearchScope.EMPTY_SCOPE
            }
        }
    }
}
