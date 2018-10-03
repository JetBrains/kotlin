/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage.version

/**
 * Manages cache attributes values.
 *
 * Attribute values can be loaded by calling [loadActual].
 * Based on loaded actual and fixed [expected] values [CacheAttributesDiff] can be constructed which can calculate [CacheStatus].
 * Build system may perform required actions based on that (i.e. rebuild something, clearing caches, etc...).
 *
 * [CacheAttributesDiff] can be used to cache current attribute values and then can be used as facade for cache version operations.
 */
interface CacheAttributesManager<Attrs : Any> {
    /**
     * Cache attribute values expected by the current version of build system and compiler.
     * `null` means that cache is not required (incremental compilation is disabled).
     */
    val expected: Attrs?

    /**
     * Load actual cache attribute values.
     * `null` means that cache is not yet created.
     *
     * This is internal operation that should be implemented by particular implementation of CacheAttributesManager.
     * Consider using `loadDiff().actual` for getting actual values.
     */
    fun loadActual(): Attrs?

    /**
     * Write [values] as cache attributes for next build execution.
     *
     * This is internal operation that should be implemented by particular implementation of CacheAttributesManager.
     * Consider using `loadDiff().saveExpectedIfNeeded()` for saving attributes values for next build.
     */
    fun writeActualVersion(values: Attrs?)

    /**
     * Check if cache with [actual] attributes values can be used when [expected] attributes are required.
     */
    fun isCompatible(actual: Attrs, expected: Attrs): Boolean = actual == expected
}

fun <Attrs : Any> CacheAttributesManager<Attrs>.loadDiff(
    actual: Attrs? = this.loadActual(),
    expected: Attrs? = this.expected
) = CacheAttributesDiff(this, actual, expected)

fun <Attrs : Any> CacheAttributesManager<Attrs>.loadAndCheckStatus() =
    loadDiff().status

/**
 * This method is kept only for compatibility.
 * Save [expected] cache attributes values if it is enabled and not equals to [actual].
 */
@Deprecated(
    message = "Consider using `this.loadDiff().saveExpectedIfNeeded()` and cache `loadDiff()` result.",
    replaceWith = ReplaceWith("loadDiff().saveExpectedIfNeeded()")
)
fun <Attrs : Any> CacheAttributesManager<Attrs>.saveIfNeeded(
    actual: Attrs? = this.loadActual(),
    expected: Attrs = this.expected
        ?: error("To save disabled cache status [delete] should be called (this behavior is kept for compatibility)")
) = loadDiff(actual, expected).saveExpectedIfNeeded()

/**
 * This method is kept only for compatibility.
 * Delete actual cache attributes values if it existed.
 */
@Deprecated(
    message = "Consider using `this.loadDiff().saveExpectedIfNeeded()` and cache `loadDiff()` result.",
    replaceWith = ReplaceWith("writeActualVersion(null)")
)
fun CacheAttributesManager<*>.clean() {
    writeActualVersion(null)
}