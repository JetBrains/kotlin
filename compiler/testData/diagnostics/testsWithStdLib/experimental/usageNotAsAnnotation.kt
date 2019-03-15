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

fun f1(e: <!EXPERIMENTAL_CAN_ONLY_BE_USED_AS_ANNOTATION!>Experimental<!>) {}
fun f2(u: <!EXPERIMENTAL_CAN_ONLY_BE_USED_AS_ANNOTATION!>UseExperimental<!>?) {}

typealias Experimental0 = <!EXPERIMENTAL_CAN_ONLY_BE_USED_AS_ANNOTATION!>Experimental<!>
typealias UseExperimental0 = <!EXPERIMENTAL_CAN_ONLY_BE_USED_AS_ANNOTATION!>UseExperimental<!>
fun f3(e: Experimental0 /* TODO */) {}
fun f4(u: UseExperimental0 /* TODO */) {}


// Usages as ::class literals should be errors

annotation class VarargKClasses(vararg val k: KClass<*>)

@VarargKClasses(
    <!EXPERIMENTAL_CAN_ONLY_BE_USED_AS_ANNOTATION!>Experimental<!>::class,
    <!EXPERIMENTAL_CAN_ONLY_BE_USED_AS_ANNOTATION!>UseExperimental<!>::class,
    kotlin.<!EXPERIMENTAL_CAN_ONLY_BE_USED_AS_ANNOTATION!>Experimental<!>::class,
    kotlin.<!EXPERIMENTAL_CAN_ONLY_BE_USED_AS_ANNOTATION!>UseExperimental<!>::class
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

fun f6(m: <!EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL!>Marker<!>) {}
fun f7(): List<<!EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL!>Marker<!>>? = null
fun f8(): test.<!EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL!>Marker<!>? = null

typealias Marker0 = <!EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL!>Marker<!>

fun f9(m: <!EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL!>Marker0<!>) {}


// Usages of markers as qualifiers are errors as well (we can lift this restriction for select cases)

fun f10(m: <!EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL!>Marker<!>.NestedClass) {
    <!EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL!>Marker<!>.value
}

// FILE: usage-from-other-file.kt

// Usages of markers in import statements should be OK, but not as qualifiers to import their nested classes

import test.Marker
import test.<!EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL!>Marker<!>.NestedClass
import test.<!EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL!>Marker<!>.Companion
