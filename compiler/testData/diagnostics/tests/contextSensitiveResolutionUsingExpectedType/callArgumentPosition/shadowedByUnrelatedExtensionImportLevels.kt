// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89822
// FILE: b.kt
package b

class MyClass {
    companion object
}

val MyClass.Companion.bar: MyClass get() = MyClass()

fun foo(a: MyClass) {}

// FILE: c.kt
package c

val Any.bar: String get() = ""

// FILE: starImportAndExplicitHider.kt
package starImportAndExplicitHider

import b.*
import c.bar

fun test() {
    foo(<!UNRESOLVED_REFERENCE!>bar<!>)
}

// FILE: bothStarImported.kt
package bothStarImported

import b.*
import c.*

fun test() {
    // both imports sit at the same scope level, so `MyClass.bar` takes the extension with
    // the more specific receiver (`MyClass.Companion` over `Any`); an explicit import outranks a star import
    foo(bar)
}

// FILE: explicitlyImportedAndHiderInPackage.kt
package explicitlyImportedAndHiderInPackage

import b.*
import b.bar

val Any.bar: String get() = ""

fun test() {
    foo(bar)
}

// FILE: bothExplicitlyImported.kt
package bothExplicitlyImported

import b.foo
import b.bar
import c.bar

fun test() {
    // both imports sit at the same scope level, so `MyClass.bar` takes the extension with
    // the more specific receiver (`MyClass.Companion` over `Any`); an explicit import outranks a star import
    foo(bar)
}

// FILE: samePackageAndStarImportedHider.kt
package b

import c.*

fun test() {
    foo(bar)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, getter, objectDeclaration,
propertyDeclaration, propertyWithExtensionReceiver */
