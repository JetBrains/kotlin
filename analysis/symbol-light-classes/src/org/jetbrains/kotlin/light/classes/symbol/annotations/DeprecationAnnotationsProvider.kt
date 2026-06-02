/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.name.JvmStandardClassIds

/**
 * Adds a synthetic `@java.lang.Deprecated` annotation when the owner is deprecated from the Kotlin point of view
 * (i.e., it or its corresponding property is annotated with [kotlin.Deprecated]).
 *
 * The Kotlin compiler marks such declarations with the JVM `Deprecated` attribute (`ACC_DEPRECATED`; see
 * `FunctionCodegen` and `compiler/testData/codegen/bytecodeListing/javaDeprecated.txt`). When the resulting bytecode is
 * read back into Java PSI, that attribute is surfaced as a `@java.lang.Deprecated` annotation. Light classes are
 * expected to mirror the bytecode, so the annotation has to be present even though there is no explicit
 * `@java.lang.Deprecated` in the source code (KT-60993).
 *
 * If the declaration already carries an explicit `@java.lang.Deprecated`, no duplicate is added (the qualifier is
 * deduplicated via [foundQualifiers]).
 */
internal class DeprecationAnnotationsProvider(private val lazyIsDeprecated: Lazy<Boolean>) : AdditionalAnnotationsProvider {
    constructor(initializer: () -> Boolean) : this(lazyPub(initializer))

    override fun addAllAnnotations(
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiElement,
    ) {
        if (!lazyIsDeprecated.value) return
        addSimpleAnnotationIfMissing(JAVA_LANG_DEPRECATED, currentRawAnnotations, foundQualifiers, owner)
    }

    override fun findSpecialAnnotation(
        annotationsBox: GranularAnnotationsBox,
        qualifiedName: String,
        owner: PsiElement,
    ): PsiAnnotation? {
        if (!lazyIsDeprecated.value) return null
        return createSimpleAnnotationIfMatches(qualifiedName, JAVA_LANG_DEPRECATED, owner)
    }

    override fun isSpecialQualifier(qualifiedName: String): Boolean = false

    private companion object {
        private val JAVA_LANG_DEPRECATED: String = JvmStandardClassIds.Annotations.Java.Deprecated.asFqNameString()
    }
}
