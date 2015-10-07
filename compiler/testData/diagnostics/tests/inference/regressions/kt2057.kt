// !DIAGNOSTICS: -UNUSED_PARAMETER

import java.util.ArrayList

fun <T> foo(a : T, b : Collection<T>, c : Int) {
}

fun <T> arrayListOf(vararg values: T): ArrayList<T> = throw Exception("$values")

val bar = foo("", arrayListOf(),<!SYNTAX!><!> )
val bar2 = foo<String>("", arrayListOf(),<!SYNTAX!><!> )