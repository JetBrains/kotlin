// WITH_FIR_TEST_COMPILER_PLUGIN
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common

// FILE: MyInlineable.kt
package org.jetbrains.kotlin.plugin.sandbox

/**
 * IMPORTANT!
 *
 * This is our own copy of the same-named annotation from the 'plugin-annotations' jar.
 *
 * We use it because it's currently problematic to attach this jar as a dependency to a common module.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.PROPERTY_GETTER)
annotation class MyInlineable

// FILE: Common.kt
package test

import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

@MyInlineable
public fun Foo(text: @MyInlineable () -> Unit) {}

@MyInlineable
fun Bar() {
    Fo<caret>o {}
}
