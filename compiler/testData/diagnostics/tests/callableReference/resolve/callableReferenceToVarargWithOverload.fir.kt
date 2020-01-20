// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KFunction2

class Foo

fun test(fn: KFunction2<Foo, Array<out String>, String>) = null

fun Foo.bar(vararg x: String) = ""
fun Foo.bar(vararg x: Int) = ""


fun actualTest() {
    <!INAPPLICABLE_CANDIDATE!>test<!>(<!UNRESOLVED_REFERENCE!>Foo::bar<!>)
}
