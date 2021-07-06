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

public abstract class KtAnnotationCall : ValidityTokenOwner {
    public abstract val classId: ClassId?
    public abstract val useSiteTarget: AnnotationUseSiteTarget?
    public abstract val psi: KtCallElement?
    public abstract val arguments: List<KtNamedConstantValue>
}

public data class KtNamedConstantValue(val name: String, val expression: KtConstantValue)

public interface KtAnnotatedSymbol : KtSymbol {
    public val annotations: List<KtAnnotationCall>

    public fun containsAnnotation(classId: ClassId): Boolean
    public val annotationClassIds: Collection<ClassId>
}