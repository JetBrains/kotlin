/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS
)
@RequiresOptIn("Internal Analysis API component which should not be used outside the Analysis API implementation modules as it does not have any compatibility guarantees")
public annotation class KaAnalysisApiInternals

@Suppress("OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN")
public typealias KtAnalysisApiInternals = KaAnalysisApiInternals

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS)
@RequiresOptIn("Internal Analysis API component which is used from other modules in the Kotlin project and is not supposed to be used anywhere else since it has no compatibility guarantees")
public annotation class KaAnalysisNonPublicApi

@Suppress("OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN")
public typealias KtAnalysisNonPublicApi = KaAnalysisNonPublicApi
