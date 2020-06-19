/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlinx.cli

import kotlin.annotation.AnnotationTarget.*

/**
 * This annotation marks the experimental API for working with command line arguments.
 *
 * > Beware using the annotated API especially if you're developing a library, since your library might become binary incompatible
 * with the future versions of the CLI library.
 *
 * Any usage of a declaration annotated with `@ExperimentalCli` must be accepted either by
 * annotating that usage with the [UseExperimental] annotation, e.g. `@UseExperimental(ExperimentalCli::class)`,
 * or by using the compiler argument `-Xuse-experimental=kotlinx.cli.ExperimentalCli`.
 */
@RequiresOptIn("This API is experimental. It may be changed in the future without notice.", RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
@Target(
        CLASS,
        ANNOTATION_CLASS,
        PROPERTY,
        FIELD,
        LOCAL_VARIABLE,
        VALUE_PARAMETER,
        CONSTRUCTOR,
        FUNCTION,
        PROPERTY_GETTER,
        PROPERTY_SETTER,
        TYPEALIAS
)

public annotation class ExperimentalCli