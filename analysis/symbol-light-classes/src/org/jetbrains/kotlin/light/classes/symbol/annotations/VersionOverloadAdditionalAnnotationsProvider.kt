/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.light.classes.symbol.methods.VersionOverload
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtElement

/**
 * A version compatibility overload doesn't exist in the source code, so the `@Deprecated` annotation which the JVM
 * backend generates for it has to be synthesized here as well.
 *
 * Mirrors `org.jetbrains.kotlin.backend.common.lower.VersionOverloadsLowering.buildDeprecationCall`.
 */
internal object VersionOverloadAdditionalAnnotationsProvider : AdditionalAnnotationsProvider {
    private val DEPRECATED_QUALIFIER: String = StandardClassIds.Annotations.Deprecated.asFqNameString()

    override fun addAllAnnotations(
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiElement,
    ) {
        val versionOverload = owner.versionOverload ?: return

        // Unlike the other providers, the annotation is added even if the declaration is deprecated on its own,
        // as the JVM backend copies the source annotations before appending the generated one
        foundQualifiers += DEPRECATED_QUALIFIER
        currentRawAnnotations += deprecationAnnotation(versionOverload, owner)
    }

    override fun findSpecialAnnotation(
        annotationsBox: GranularAnnotationsBox,
        qualifiedName: String,
        owner: PsiElement,
    ): PsiAnnotation? = null

    override fun isSpecialQualifier(qualifiedName: String): Boolean = false

    override fun canAddAnnotation(qualifiedName: String): Boolean = qualifiedName == DEPRECATED_QUALIFIER

    private fun deprecationAnnotation(versionOverload: VersionOverload, owner: PsiElement): PsiAnnotation =
        SymbolLightSimpleAnnotation(
            fqName = DEPRECATED_QUALIFIER,
            parent = owner,
            arguments = listOf(
                AnnotationArgument(
                    name = Name.identifier("message"),
                    value = AnnotationValue.Constant(GeneratedStringConstantValue(versionOverload.deprecationMessage), sourcePsi = null),
                ),
                AnnotationArgument(
                    name = Name.identifier("level"),
                    value = AnnotationValue.EnumValue(
                        callableId = CallableId(StandardClassIds.DeprecationLevel, Name.identifier("ERROR")),
                        sourcePsi = null,
                    ),
                ),
            ),
        )
}

private val PsiElement.versionOverload: VersionOverload?
    get() = (parent as? SymbolLightMethodBase)?.versionOverload

/**
 * A [String] constant which has no counterpart in the source code.
 */
@OptIn(KaImplementationDetail::class)
private class GeneratedStringConstantValue(override val value: String) : KaConstantValue.StringValue {
    override val sourcePsi: KtElement? get() = null
    override fun render(): String = "\"$value\""
}
