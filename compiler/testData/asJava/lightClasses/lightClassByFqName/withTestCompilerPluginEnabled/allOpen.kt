// test.BaseClass
// WITH_FIR_TEST_COMPILER_PLUGIN
// IGNORE_LIBRARY_EXCEPTIONS: KT-58535
// FILE: main.kt
package test

import org.jetbrains.kotlin.plugin.sandbox.AllOpen

@AllOpen
class BaseClass {
    fun function() {}

    var prop: Int = 42
}

// FILE: AllOpen.kt
package org.jetbrains.kotlin.plugin.sandbox

/**
 * Imitates AllOpen annotation to trigger AllOpenMatcherBasedStatusTransformer
 */
annotation class AllOpen
