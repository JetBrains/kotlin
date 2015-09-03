// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo() {}
fun foo(s: String) {}

fun bar(f: () -> Unit) = 1
fun bar(f: (String) -> Unit) = 2

val x1 = ::foo <!USELESS_CAST!>as () -> Unit<!>
val x2 = bar(<!UNCHECKED_CAST!>::foo as (String) -> Unit<!>)