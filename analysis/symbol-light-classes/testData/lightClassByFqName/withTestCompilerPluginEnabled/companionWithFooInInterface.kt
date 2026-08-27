// test.MyInterface
// WITH_FIR_TEST_COMPILER_PLUGIN

// FILE: main.kt
package test

import org.jetbrains.kotlin.plugin.sandbox.CompanionWithFoo

@CompanionWithFoo
interface MyInterface

// FILE: CompanionWithFoo.kt
package org.jetbrains.kotlin.plugin.sandbox

/**
 * Imitates the annotation from the plugin annotations artifact, which is not available on all platforms.
 * Triggers CompanionGenerator.
 */
annotation class CompanionWithFoo
