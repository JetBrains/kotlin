// WITH_FIR_TEST_COMPILER_PLUGIN

// FILE: main.kt
package test

import org.jetbrains.kotlin.plugin.sandbox.CompanionWithFoo

@CompanionWithFoo
class MyClass

// FILE: CompanionWithFoo.kt
package org.jetbrains.kotlin.plugin.sandbox

/**
 * Shadows the annotation of the same name from the plugin annotations artifact,
 * which is only available on the JVM classpath.
 */
annotation class CompanionWithFoo
