/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.utils

import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.gradle.KotlinCompilation
import org.jetbrains.kotlin.gradle.KotlinModule
import org.jetbrains.kotlin.gradle.compilationFullName
import org.jetbrains.kotlin.idea.configuration.KotlinMPPGradleProjectResolver
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext

internal fun KotlinModule.fullName(simpleName: String = name) = when (this) {
    is KotlinCompilation -> compilationFullName(simpleName, disambiguationClassifier)
    else -> simpleName
}

internal fun KotlinMPPGradleProjectResolver.Companion.getKotlinModuleId(
    gradleModule: IdeaModule, kotlinModule: KotlinModule, resolverCtx: ProjectResolverContext
) = getGradleModuleQualifiedName(resolverCtx, gradleModule, kotlinModule.fullName())