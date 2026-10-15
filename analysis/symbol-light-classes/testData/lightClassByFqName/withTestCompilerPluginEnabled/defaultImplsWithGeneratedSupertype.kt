// test.Annotated
// WITH_FIR_TEST_COMPILER_PLUGIN

// FILE: main.kt
package test

import org.jetbrains.kotlin.plugin.sandbox.MyInterfaceSupertype

@MyInterfaceSupertype
interface Annotated

// FILE: MyInterface.kt
package foo

interface MyInterface {
    fun foo() {}
}

// FILE: MyInterfaceSupertype.kt
package org.jetbrains.kotlin.plugin.sandbox

/**
 * Imitates the annotation from the plugin annotations artifact, which is not available on all platforms.
 * Triggers SomeAdditionalSupertypeGenerator, which adds `foo.MyInterface` as a supertype.
 */
annotation class MyInterfaceSupertype
