// !LANGUAGE: +NestedClassesInAnnotations
// !USE_EXPERIMENTAL: kotlin.Experimental
// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: api.kt

package test

import kotlin.reflect.KClass

// Usages in import should be OK
import kotlin.Experimental.Level.*
import kotlin.Experimental.Level
import kotlin.Experimental

// Usages with FQ names should be OK

@kotlin.Experimental(kotlin.Experimental.Level.ERROR)
annotation class M


// Usages as types should be errors

fun f1(e: Experimental) {}
fun f2(u: UseExperimental?) {}

typealias Experimental0 = Experimental
typealias UseExperimental0 = UseExperimental
fun f3(e: Experimental0 /* TODO */) {}
fun f4(u: UseExperimental0 /* TODO */) {}


// Usages as ::class literals should be errors

annotation class VarargKClasses(vararg val k: KClass<*>)

@VarargKClasses(
    Experimental::class,
    UseExperimental::class,
    kotlin.Experimental::class,
    kotlin.UseExperimental::class
)
fun f5() {}


// Usages of markers as types should be errors

@Experimental
annotation class Marker {
    class NestedClass

    companion object {
        const val value = 42
    }
}

fun f6(m: Marker) {}
fun f7(): List<Marker>? = null
fun f8(): test.Marker? = null

typealias Marker0 = Marker

fun f9(m: Marker0) {}


// Usages of markers as qualifiers are errors as well (we can lift this restriction for select cases)

fun f10(m: Marker.NestedClass) {
    Marker.value
}

// FILE: usage-from-other-file.kt

// Usages of markers in import statements should be OK, but not as qualifiers to import their nested classes

import test.Marker
import test.Marker.NestedClass
import test.Marker.Companion
