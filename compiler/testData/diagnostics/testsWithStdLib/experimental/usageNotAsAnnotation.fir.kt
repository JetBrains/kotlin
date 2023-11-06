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
fun f2(u: <!OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION!>OptIn?<!>) {}

typealias Experimental0 = <!OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION!>RequiresOptIn<!>
typealias OptIn0 = <!OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION!>OptIn<!>
fun f3(e: <!OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION!>Experimental0<!> /* TODO */) {}
fun f4(u: <!OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION!>OptIn0<!> /* TODO */) {}


// Usages as ::class literals should be errors

annotation class VarargKClasses(vararg val k: KClass<*>)

@VarargKClasses(
    <!OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION!>RequiresOptIn<!>::class,
    <!OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION!>OptIn<!>::class,
    <!OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION!>kotlin.RequiresOptIn<!>::class,
    <!OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION!>kotlin.OptIn<!>::class
)
fun f5() {}


// Usages of markers as types should be errors

object A {
    @RequiresOptIn
    annotation class Marker {
        class NestedClass() {
            class NestedClass2

            fun f12(m: NestedClass2){
                val x = value
                bar()
            }
        }

        companion object {
            const val value = 42

            fun bar(){}
        }
    }
}

fun f6(m: <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>A.Marker<!>) {}
fun f7(): List<<!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>A.Marker<!>>? = null
fun f8(): <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>test.A.Marker?<!> = null

typealias Marker0 = <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>A.Marker<!>

typealias Marker1 = <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>Marker0<!>

fun f9(m: <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>Marker0<!>) {}


// Usages of markers as qualifiers are errors as well (we can lift this restriction for select cases)

fun f10(m: <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>A.Marker.NestedClass<!>) {
    val a = <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>A.Marker.value<!>
    <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>A.Marker.Companion.value<!>
    <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>A.Marker.bar()<!>
    <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>A.Marker.Companion.bar()<!>
}

fun f11(m: <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>A.Marker.NestedClass.NestedClass2<!>) {}


// FILE: usage-from-other-file.kt

// Usages of markers in import statements should be OK, but not as qualifiers to import their nested classes

import test.A.Marker
<!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>import test.A.Marker.NestedClass<!>
<!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>import test.A.Marker.NestedClass.NestedClass2<!>
<!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>import test.A.Marker.Companion<!>
<!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>import test.A.Marker.Companion.value<!>

// FILE: usage-from-other-file-2.kt
import test.Marker0
<!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>import test.Marker0.NestedClass<!>

// FILE: usage-from-other-file-3.kt
import test.Marker1
<!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>import test.Marker1.NestedClass<!>

fun f12(m: <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>test.A.Marker.NestedClass.NestedClass2<!>) {}
