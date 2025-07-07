/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.declarations

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinJvmClassIdProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinJvmClassIdProviderFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KotlinStandaloneFirJvmClassIdProvider : KotlinJvmClassIdProvider {
    override fun getTopLevelClassIdsByShortName(shortName: Name): Set<ClassId>? = null
}

internal class KotlinStandaloneFirJvmClassIdProviderFactory(private val project: Project) : KotlinJvmClassIdProviderFactory {
    override fun createProvider(scope: GlobalSearchScope): KotlinJvmClassIdProvider = KotlinStandaloneFirJvmClassIdProvider()
}
