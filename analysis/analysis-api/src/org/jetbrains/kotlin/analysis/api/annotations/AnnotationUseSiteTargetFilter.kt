/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget

public fun interface AnnotationUseSiteTargetFilter {
    public fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean
}

public fun AnnotationUseSiteTarget?.toFilter(): AnnotationUseSiteTargetFilter = when (this) {
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

public object AnyAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = true
}

public object NoAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = useSiteTarget == null
}

public object FieldAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = useSiteTarget == AnnotationUseSiteTarget.FIELD
}

public object FileAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = useSiteTarget == AnnotationUseSiteTarget.FILE
}

public object PropertyAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = useSiteTarget == AnnotationUseSiteTarget.PROPERTY
}

public object PropertyGetterAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = useSiteTarget == AnnotationUseSiteTarget.PROPERTY_GETTER
}

public object PropertySetterAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = useSiteTarget == AnnotationUseSiteTarget.PROPERTY_SETTER
}

public object ReceiverAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = useSiteTarget == AnnotationUseSiteTarget.RECEIVER
}

public object ConstructorParameterAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean =
        useSiteTarget == AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
}

public object SetterParameterAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean = useSiteTarget == AnnotationUseSiteTarget.SETTER_PARAMETER
}

public object PropertyDelegateFieldAnnotationUseSiteTargetFilter : AnnotationUseSiteTargetFilter {
    override fun isAllowed(useSiteTarget: AnnotationUseSiteTarget?): Boolean =
        useSiteTarget == AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
}
