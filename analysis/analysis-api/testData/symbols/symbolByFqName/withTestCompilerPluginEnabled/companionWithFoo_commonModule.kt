// WITH_FIR_TEST_COMPILER_PLUGIN

// MODULE: common
// TARGET_PLATFORM: Common

// FILE: myAnnotations.kt
package org.jetbrains.kotlin.plugin.sandbox

/**
 * IMPORTANT!
 *
 * This is our own copy of the same-named annotation from the 'plugin-annotations' jar.
 *
 * We use it because it's currently problematic to attach this jar as a dependency to a common module.
 */
annotation class CompanionWithFoo

// FILE: main.kt
package test

@org.jetbrains.kotlin.plugin.sandbox.CompanionWithFoo
class WithGeneratedCompanion

// class: test/WithGeneratedCompanion.Companion
