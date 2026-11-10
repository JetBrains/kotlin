/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.abi

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.buildtools.api.abi.AbiFilters
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.daemon.cast
import org.jetbrains.kotlin.buildtools.internal.daemon.throwPropertyNotFound

@Serializable
internal class AbiFiltersImpl() : AbiFilters, AbiFilters.Builder, DeepCopyable<AbiFilters> {

    private constructor(other: AbiFiltersImpl) : this() {
        includeNamed = other.includeNamed.toSet()
        excludeNamed = other.excludeNamed.toSet()
        includeAnnotatedWith = other.includeAnnotatedWith.toSet()
        excludeAnnotatedWith = other.excludeAnnotatedWith.toSet()
    }

    @Suppress("UNCHECKED_CAST")
    @UseFromImplModuleRestricted
    override fun <V> get(key: AbiFilters.Option<V>): V = when (key.id) {
        "INCLUDE_NAMED" -> includeNamed
        "EXCLUDE_NAMED" -> excludeNamed
        "INCLUDE_ANNOTATED_WITH" -> includeAnnotatedWith
        "EXCLUDE_ANNOTATED_WITH" -> excludeAnnotatedWith
        else -> throwPropertyNotFound(key.id)
    } as V

    @UseFromImplModuleRestricted
    override fun <V> set(key: AbiFilters.Option<V>, value: V) {
        when (key.id) {
            "INCLUDE_NAMED" -> includeNamed = value.cast()
            "EXCLUDE_NAMED" -> excludeNamed = value.cast()
            "INCLUDE_ANNOTATED_WITH" -> includeAnnotatedWith = value.cast()
            "EXCLUDE_ANNOTATED_WITH" -> excludeAnnotatedWith = value.cast()
            else -> throwPropertyNotFound(key.id)
        }
    }

    override fun deepCopy(): AbiFiltersImpl = AbiFiltersImpl(this)

    override fun build(): AbiFilters {
        return deepCopy()
    }

    /**
     * Include a class, file-level property, or file-level function in a dump by its name.
     * Declarations that do not match the specified names, that do not have an annotation from [INCLUDE_ANNOTATED_WITH]
     * and do not have members marked with an annotation from [INCLUDE_ANNOTATED_WITH] are excluded from the dump.
     *
     * The name filter compares the qualified class name with the value in the filter:
     *
     * For Kotlin declarations, fully qualified names are used.
     * It is important to keep in mind that dots are used everywhere as separators, even in the case of a nested class.
     * E.g. for qualified name `foo.bar.Container.Value`, here `Value` is a class nested in `Container`.
     *
     * For classes from Java sources, canonical names are used.
     * The main motivation is a similar approach to writing the class name - dots are used everywhere as delimiters.
     *
     * Name templates are allowed, with support for wildcards such as `**`, `*`, and `?`:
     * - `**` - zero or any number of characters
     * - `*` - zero or any number of characters excluding dot. Using to specify simple class name.
     * - `?` - any single character.
     */
    var includeNamed: Set<String> = emptySet()

    /**
     * Excludes a class, file-level property, or file-level function from a dump by its name.
     *
     * The name filter compares the qualified class name with the value in the filter:
     *
     * For Kotlin declarations, fully qualified names are used.
     * It is important to keep in mind that dots are used everywhere as separators, even in the case of a nested class.
     * E.g. for qualified name `foo.bar.Container.Value`, here `Value` is a class nested in `Container`.
     *
     * For classes from Java sources, canonical names are used.
     * The main motivation is a similar approach to writing the class name - dots are used everywhere as delimiters.
     *
     * Name templates are allowed, with support for wildcards such as `**`, `*`, and `?`:
     * - `**` - zero or any number of characters
     * - `*` - zero or any number of characters excluding dot. Using to specify simple class name.
     * - `?` - any single character.
     */
    var excludeNamed: Set<String> = emptySet()

    /**
     * Includes a declaration by annotations placed on it.
     *
     * Any declaration that is not marked with one of the these annotations and does not match the [INCLUDE_NAMED] is excluded from the dump.
     *
     * The declaration can be a class, a class member (function or property), a top-level function or a top-level property.
     *
     * Name templates are allowed, with support for wildcards such as `**`, `*`, and `?`:
     * - `**` - zero or any number of characters
     * - `*` - zero or any number of characters excluding dot. Using to specify simple class name.
     * - `?` - any single character.
     *
     * The annotation should not have [Retention] equal to [AnnotationRetention.SOURCE], otherwise, filtering by it will not work.
     */
    var includeAnnotatedWith: Set<String> = emptySet()

    /**
     * Excludes a declaration by annotations placed on it.
     *
     * It means that a class, a class member (function or property), a top-level function, or a top-level property
     * marked by a specific annotation will be excluded from the dump.
     *
     * Name templates are allowed, with support for wildcards such as `**`, `*`, and `?`:
     * - `**` - zero or any number of characters
     * - `*` - zero or any number of characters excluding dot. Using to specify simple class name.
     * - `?` - any single character.
     *
     * The annotation should not have [Retention] equal to [AnnotationRetention.SOURCE], otherwise, filtering by it will not work.
     */
    var excludeAnnotatedWith: Set<String> = emptySet()
}
