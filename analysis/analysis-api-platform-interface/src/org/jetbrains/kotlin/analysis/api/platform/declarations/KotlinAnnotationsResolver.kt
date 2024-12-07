/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.declarations

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtElement

/**
 * [KotlinAnnotationsResolver] matches declarations with their annotations and vice versa.
 *
 * This service can produce both false positives and false negatives, since it might not be allowed to use full resolution to understand the
 * true [FqName][org.jetbrains.kotlin.name.FqName] of a used annotation.
 *
 * The next statement should be `true` for any `annotation`:
 *
 * ```
 * declarationsByAnnotation(annotation).all { declaration ->
 *   annotation in annotationsOnDeclaration(declaration)
 * }
 * ```
 */
public interface KotlinAnnotationsResolver {
    /**
     * Returns an approximate set of [KtAnnotated] declarations which have an annotation with the given [annotationClassId] applied to them.
     * The set may contain both false positives and false negatives.
     */
    public fun declarationsByAnnotation(annotationClassId: ClassId): Set<KtAnnotated>

    /**
     * Returns an approximate set of annotation [ClassId]s which have been applied to [declaration]. The set may contain both false
     * positives and false negatives.
     *
     * @param declaration A [KtDeclaration][org.jetbrains.kotlin.psi.KtDeclaration] or [KtFile][org.jetbrains.kotlin.psi.KtFile] to resolve
     *  annotations on. Other [KtElement]s are not supported.
     */
    public fun annotationsOnDeclaration(declaration: KtAnnotated): Set<ClassId>
}

public interface KotlinAnnotationsResolverFactory : KotlinPlatformComponent {
    /**
     * @param searchScope A scope in which the created [KotlinAnnotationsResolver] will operate. Make sure that this scope contains all
     *  the annotations that you might want to resolve.
     */
    public fun createAnnotationResolver(searchScope: GlobalSearchScope): KotlinAnnotationsResolver

    public companion object {
        public fun getInstance(project: Project): KotlinAnnotationsResolverFactory = project.service()
    }
}

public fun Project.createAnnotationResolver(searchScope: GlobalSearchScope): KotlinAnnotationsResolver =
    KotlinAnnotationsResolverFactory.getInstance(this).createAnnotationResolver(searchScope)
