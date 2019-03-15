/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Project

// KT-29613, KT-29783
internal object KotlinNativeHomeEvaluator {
    private const val KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY = "konanHome"

    private const val FALLBACK_ACCESSOR_CLASS = "org.jetbrains.kotlin.compilerRunner.KotlinNativeToolRunnerKt"
    private const val FALLBACK_ACCESSOR_METHOD = "getKonanHome"

    internal fun getKotlinNativeHome(project: Project): String? =
        getKotlinNativeHomePrimary(project) ?: getKotlinNativeHomeFallback(project)

    // Read Kotlin/Native home from the predefined property in Gradle plugin.
    // Should work for Gradle plugin with version >= 1.3.20.
    private fun getKotlinNativeHomePrimary(project: Project) = project.findProperty(KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY) as String?

    // Evaluate Kotlin/Native home using reflection by internal val declared in Gradle plugin.
    // This should work for Gradle plugin with version < 1.3.20.
    private fun getKotlinNativeHomeFallback(project: Project): String? {
        val kotlinExtensionClassLoader = project.extensions.findByName("kotlin")?.javaClass?.classLoader ?: return null
        val accessorClass = try {
            Class.forName(FALLBACK_ACCESSOR_CLASS, true, kotlinExtensionClassLoader)
        } catch (e: ClassNotFoundException) {
            return null
        }
        val accessorMethod = accessorClass.getMethodOrNull(FALLBACK_ACCESSOR_METHOD, Project::class.java) ?: return null
        return accessorMethod.invoke(null, project) as String
    }
}
