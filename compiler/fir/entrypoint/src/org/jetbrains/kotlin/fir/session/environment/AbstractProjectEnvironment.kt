/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session.environment

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import java.io.File

interface AbstractProjectFileSearchScope {
    val isEmpty: Boolean

    operator fun minus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope

    operator fun plus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope

    operator fun not(): AbstractProjectFileSearchScope


    object EMPTY : AbstractProjectFileSearchScope {
        override val isEmpty: Boolean = true
        override fun minus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope = this
        override fun plus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope = other
        override fun not(): AbstractProjectFileSearchScope = ANY
    }

    object ANY : AbstractProjectFileSearchScope {
        override val isEmpty: Boolean = false
        override fun minus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope = error("Operation not implemented")
        override fun plus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope = this
        override fun not(): AbstractProjectFileSearchScope = EMPTY
    }
}

interface AbstractProjectEnvironment {
    fun getKotlinClassFinder(fileSearchScope: AbstractProjectFileSearchScope): KotlinClassFinder

    fun getJavaModuleResolver(): JavaModuleResolver

    fun getPackagePartProvider(fileSearchScope: AbstractProjectFileSearchScope): PackagePartProvider

    fun registerAsJavaElementFinder(firSession: FirSession)

    fun getSearchScopeByIoFiles(files: Iterable<File>, allowOutOfProjectRoots: Boolean = false): AbstractProjectFileSearchScope

    fun getSearchScopeByDirectories(directories: Iterable<File>): AbstractProjectFileSearchScope

    fun getSearchScopeForProjectLibraries(): AbstractProjectFileSearchScope

    fun getSearchScopeForProjectJavaSources(): AbstractProjectFileSearchScope

    fun getFirJavaFacade(
        firSession: FirSession,
        baseModuleData: FirModuleData,
        fileSearchScope: AbstractProjectFileSearchScope
    ): FirJavaFacade
}
