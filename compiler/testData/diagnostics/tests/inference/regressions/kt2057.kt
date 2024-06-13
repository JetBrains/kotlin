// LANGUAGE: -TrailingCommas
// DIAGNOSTICS: -UNUSED_PARAMETER

import java.util.ArrayList

fun <T> foo(a : T, b : Collection<T>, c : Int) {
}

fun <T> arrayListOf(vararg values: T): ArrayList<T> = throw Exception("$values")

val bar = foo("", <!NO_VALUE_FOR_PARAMETER!>arrayListOf()<!UNSUPPORTED_FEATURE!>,<!> )<!>
val bar2 = foo<String>("", <!NO_VALUE_FOR_PARAMETER!>arrayListOf()<!UNSUPPORTED_FEATURE!>,<!> )<!>
