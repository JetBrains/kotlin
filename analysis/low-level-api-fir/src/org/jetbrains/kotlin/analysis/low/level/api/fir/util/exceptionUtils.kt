/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.utils.errors.withPsiEntry
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


fun errorWithFirSpecificEntries(
    message: String,
    cause: Exception? = null,
    fir: FirElement? = null,
    coneType: ConeKotlinType? = null,
    psi: PsiElement? = null,
    additionalInfos: ExceptionAttachmentBuilder.() -> Unit = {}
): Nothing {
    errorWithAttachment(message, cause) {
        if (fir != null) {
            withFirEntry("fir", fir)
        }

        if (psi != null) {
            withPsiEntry("psi", psi, ProjectStructureProvider.getModule(psi.project, psi, contextualModule = null))
        }

        if (coneType != null) {
            withConeTypeEntry("coneType", coneType)
        }
        additionalInfos()
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <reified R> Any.requireTypeIntersectionWith() {
    contract { returns() implies (this@requireTypeIntersectionWith is R) }

    requireWithAttachment(
        this is R,
        { "${this::class.simpleName} must be ${R::class.simpleName}" },
    ) {
        if (this@requireTypeIntersectionWith is FirElement) {
            withFirEntry("container", this@requireTypeIntersectionWith)
        }
    }
}
