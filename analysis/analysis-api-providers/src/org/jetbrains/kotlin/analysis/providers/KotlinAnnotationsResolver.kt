/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtElement

/**
 * A service to match declarations with their annotations and vice versa.
 *
 * N.B. This service can produce both false positives and false negatives from time to time, since it might not be allowed to use
 * full-blown resolve to understand the true FqName of used annotation.
 *
 * The next statement should be `true` for any `annotation`:
 * ```
 * declarationsByAnnotation(annotation).all { declaration ->
 *   annotation in annotationsOnDeclaration(declaration)
 * }
 * ```
 */
public interface KotlinAnnotationsResolver {
    /**
     * @param queriedAnnotation A qualified name of the annotation in question.
     * @return A set of PSI declarations which have [queriedAnnotation] declared directly on them.
     * Might contain both false positives and false negatives.
     */
    public fun declarationsByAnnotation(queriedAnnotation: ClassId): Set<KtAnnotated>

    /**
     * @param declaration A [org.jetbrains.kotlin.psi.KtDeclaration] or [org.jetbrains.kotlin.psi.KtFile] to resolve annotations on. Other
     * [KtElement]s are not supported.
     * @return A set of annotations declared directly on the [declaration]. Might contain both false positives and false negatives.
     */
    public fun annotationsOnDeclaration(declaration: KtAnnotated): Set<ClassId>
}

public interface KotlinAnnotationsResolverFactory {
    /**
     * @param searchScope A scope in which the created [KotlinAnnotationsResolver] will operate. Make sure that this scope contains all
     * the annotations that you might want to resolve.
     */
    public fun createAnnotationResolver(searchScope: GlobalSearchScope): KotlinAnnotationsResolver
}

public fun Project.createAnnotationResolver(searchScope: GlobalSearchScope): KotlinAnnotationsResolver =
    ServiceManager.getService(this, KotlinAnnotationsResolverFactory::class.java)
        .createAnnotationResolver(searchScope)
