// WITH_FIR_TEST_COMPILER_PLUGIN
// IGNORE_FE10
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common

// FILE: MyComposable.kt
package org.jetbrains.kotlin.fir.plugin

/**
 * IMPORTANT!
 *
 * This is our own copy of the same-named annotation from the 'plugin-annotations' jar.
 *
 * We use it because it's currently problematic to attach this jar as a dependency to a common module.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.PROPERTY_GETTER)
annotation class MyComposable

// FILE: Common.kt
package test

import org.jetbrains.kotlin.fir.plugin.MyComposable

@MyComposable
public fun Foo(text: @MyComposable () -> Unit) {}

@MyComposable
fun Bar() {
    Fo<caret>o {}
}