/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.java

import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleJavaAnnotationsProvider
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleResolver
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.name.ClassId

/**
 * Delegates directly to [CliJavaModuleResolver] as we can use it in Standalone.
 *
 * Inheriting from [KotlinJavaModuleJavaAnnotationsProvider] allows the component to provide [JavaAnnotation]s directly, without having to
 * convert them to PSI annotations (and then another conversion of those PSI annotations back into [JavaAnnotation] in
 * `KaBaseJavaModuleResolver`).
 */
@OptIn(KaNonPublicApi::class)
internal class KotlinStandaloneJavaModuleAnnotationsProvider(
    private val javaModuleResolver: CliJavaModuleResolver,
) : KotlinJavaModuleJavaAnnotationsProvider {
    override fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<JavaAnnotation>? =
        javaModuleResolver.getAnnotationsForModuleOwnerOfClass(classId)
}
