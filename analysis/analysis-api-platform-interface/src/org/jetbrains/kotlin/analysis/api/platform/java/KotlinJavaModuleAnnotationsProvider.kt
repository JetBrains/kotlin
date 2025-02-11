/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.java

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.name.ClassId

/**
 * Provides annotations for Java modules.
 *
 * The platform must define one of the following annotation providers:
 *
 * - [KotlinJavaModulePsiAnnotationsProvider] (recommended)
 * - [KotlinJavaModuleJavaAnnotationsProvider]
 */
public sealed interface KotlinJavaModuleAnnotationsProvider<T> : KotlinPlatformComponent {
    /**
     * Returns all annotations of the Java module where the class with the given [classId] is defined.
     *
     * #### Example
     *
     * ```
     * // module-info.java
     * @MyAnnotation
     * module myModule {
     * }
     *
     * // MyClass.java
     * package myModule
     *
     * public class MyClass {
     * }
     * ```
     *
     * Here, when given `myModule.MyClass`, [getAnnotationsForModuleOwnerOfClass] should return `@MyAnnotation`.
     */
    public fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<T>?

    public companion object {
        public fun getInstance(project: Project): KotlinJavaModuleAnnotationsProvider<*> = project.service()
    }
}

/**
 * Provides [PsiAnnotation]s for Java modules.
 */
public interface KotlinJavaModulePsiAnnotationsProvider : KotlinJavaModuleAnnotationsProvider<PsiAnnotation>

/**
 * Directly provides [JavaAnnotation]s, which is the representation that the Kotlin compiler expects internally. However, [JavaAnnotation]
 * is a compiler-internal API, so this provider should only be preferred if the implementation directly accesses [JavaAnnotation]s.
 */
@KaNonPublicApi
public interface KotlinJavaModuleJavaAnnotationsProvider : KotlinJavaModuleAnnotationsProvider<JavaAnnotation>
