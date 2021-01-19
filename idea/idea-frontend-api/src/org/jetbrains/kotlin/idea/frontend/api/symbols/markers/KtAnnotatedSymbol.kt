/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols.markers

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallElement

abstract class KtAnnotationCall : ValidityTokenOwner {
    abstract val classId: ClassId?
    abstract val useSiteTarget: AnnotationUseSiteTarget?
    abstract val psi: KtCallElement?
    abstract val arguments: List<KtNamedConstantValue>
}

data class KtNamedConstantValue(val name: String, val expression: KtConstantValue)

interface KtAnnotatedSymbol : KtSymbol {
    val annotations: List<KtAnnotationCall>

    fun containsAnnotation(classId: ClassId): Boolean
    val annotationClassIds: Collection<ClassId>
}