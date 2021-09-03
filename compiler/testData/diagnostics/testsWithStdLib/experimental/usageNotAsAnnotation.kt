// !LANGUAGE: +NestedClassesInAnnotations
// !OPT_IN: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: api.kt

package test

import kotlin.reflect.KClass

// Usages in import should be OK
import kotlin.RequiresOptIn.Level.*
import kotlin.RequiresOptIn.Level
import kotlin.RequiresOptIn

// Usages with FQ names should be OK

@kotlin.RequiresOptIn(level = kotlin.RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
annotation class M


// Usages as types should be errors

fun f1(e: <!OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION!>RequiresOptIn<!>) {}
fun f2(u: <!OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION!>OptIn<!>?) {}

typealias Experimental0 = <!OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION!>RequiresOptIn<!>
typealias OptIn0 = <!OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION!>OptIn<!>
fun f3(e: Experimental0 /* TODO */) {}
fun f4(u: OptIn0 /* TODO */) {}


// Usages as ::class literals should be errors

annotation class VarargKClasses(vararg val k: KClass<*>)

@VarargKClasses(
    <!OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION!>RequiresOptIn<!>::class,
    <!OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION!>OptIn<!>::class,
    kotlin.<!OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION!>RequiresOptIn<!>::class,
    kotlin.<!OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION!>OptIn<!>::class
)
fun f5() {}


// Usages of markers as types should be errors

@RequiresOptIn
annotation class Marker {
    class NestedClass

    companion object {
        const val value = 42
    }
}

fun f6(m: <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>Marker<!>) {}
fun f7(): List<<!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>Marker<!>>? = null
fun f8(): test.<!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>Marker<!>? = null

typealias Marker0 = <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>Marker<!>

fun f9(m: <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>Marker0<!>) {}


// Usages of markers as qualifiers are errors as well (we can lift this restriction for select cases)

fun f10(m: <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>Marker<!>.NestedClass) {
    <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>Marker<!>.value
}

// FILE: usage-from-other-file.kt

// Usages of markers in import statements should be OK, but not as qualifiers to import their nested classes

import test.Marker
import test.<!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>Marker<!>.NestedClass
import test.<!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>Marker<!>.Companion
