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
    AnnotationTarget.TYPEALIAS,
)
@RequiresOptIn("Internal API which should not be used outside the Analysis API implementation modules as it does not have any compatibility guarantees")
public annotation class KaImplementationDetail

/**
 * TODO(KT-68883): replace with [KaImplementationDetail]
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS
)
@RequiresOptIn("Internal Analysis API component which should not be used outside the Analysis API implementation modules as it does not have any compatibility guarantees")
public annotation class KaAnalysisApiInternals

@Deprecated("Use 'KaAnalysisApiInternals' instead", ReplaceWith("KaAnalysisApiInternals"))
@Suppress("OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN")
public typealias KtAnalysisApiInternals = KaAnalysisApiInternals

/**
 * TODO(KT-68883): replace with [KaNonPublicApi]
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS)
@RequiresOptIn("Internal API which is used in projects developed by JetBrains")
public annotation class KaAnalysisNonPublicApi

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@RequiresOptIn("Internal API which is used in projects developed by JetBrains")
public annotation class KaNonPublicApi

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@RequiresOptIn("Internal API which is used only from the IntelliJ Kotlin plugin. Such an API should not be used in other places since it has no compatibility guarantees")
public annotation class KaIdeApi

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@RequiresOptIn("Experimental API with no compatibility guarantees")
public annotation class KaExperimentalApi

@Deprecated("Use 'KaNonPublicApi' instead", ReplaceWith("KaNonPublicApi"))
@Suppress("OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN")
public typealias KtAnalysisNonPublicApi = KaAnalysisNonPublicApi
