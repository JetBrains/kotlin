// !USE_EXPERIMENTAL: kotlin.Experimental
// !DIAGNOSTICS: -UNUSED_PARAMETER

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
annotation class Marker

fun f6(m: <!EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL!>Marker<!>) {}
fun f7(): List<<!EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL!>Marker<!>>? = null
fun f8(): test.<!EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL!>Marker<!>? = null

typealias Marker0 = <!EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL!>Marker<!>

fun f9(m: <!EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL!>Marker0<!>) {}
