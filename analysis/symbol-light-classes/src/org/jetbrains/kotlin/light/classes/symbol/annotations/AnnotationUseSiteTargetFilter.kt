/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget

internal fun interface AnnotationUseSiteTargetFilter {
    fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean
}

internal fun AnnotationUseSiteTarget?.toFilter(): AnnotationUseSiteTargetFilter = when (this) {
    null -> NoAnnotationUseSiteTargetFilter
    AnnotationUseSiteTarget.FIELD -> FieldAnnotationUseSiteTargetFilter
    AnnotationUseSiteTarget.FILE -> FileAnnotationUseSiteTargetFilter
    AnnotationUseSiteTarget.PROPERTY -> PropertyAnnotationUseSiteTargetFilter
    AnnotationUseSiteTarget.PROPERTY_GETTER -> PropertyGetterAnnotationUseSiteTargetFilter
    AnnotationUseSiteTarget.PROPERTY_SETTER -> PropertySetterAnnotationUseSiteTargetFilter
    AnnotationUseSiteTarget.RECEIVER -> ReceiverAnnotationUseSiteTargetFilter
    AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER -> ConstructorParameterAnnotationUseSiteTargetFilter
    AnnotationUseSiteTarget.SETTER_PARAMETER -> SetterParameterAnnotationUseSiteTargetFilter
    AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD -> PropertyDelegateFieldAnnotationUseSiteTargetFilter
}

internal object AnyAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = true
}

internal object NoAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = useSiteTarget == null
}

internal object FieldAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = useSiteTarget == AnnotationUseSiteTarget.FIELD
}

internal object FileAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = useSiteTarget == AnnotationUseSiteTarget.FILE
}

internal object PropertyAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = useSiteTarget == AnnotationUseSiteTarget.PROPERTY
}

internal object PropertyGetterAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = useSiteTarget == AnnotationUseSiteTarget.PROPERTY_GETTER
}

internal object PropertySetterAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = useSiteTarget == AnnotationUseSiteTarget.PROPERTY_SETTER
}

internal object ReceiverAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = useSiteTarget == AnnotationUseSiteTarget.RECEIVER
}

internal object ConstructorParameterAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean =
        useSiteTarget == AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
}

internal object SetterParameterAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = useSiteTarget == AnnotationUseSiteTarget.SETTER_PARAMETER
}

internal object PropertyDelegateFieldAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean =
        useSiteTarget == AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
}