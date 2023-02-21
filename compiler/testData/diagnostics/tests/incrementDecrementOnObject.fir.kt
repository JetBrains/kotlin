// SKIP_TXT
// ISSUE: KT-56659

object AAA { operator fun inc(): AAA = this }

fun test1() {
    <!VARIABLE_EXPECTED!>AAA<!>++
}

fun test2() {
    ++<!VARIABLE_EXPECTED!>AAA<!>
}

fun test3() {
    var x = AAA
    x = <!VARIABLE_EXPECTED!>AAA<!>++
}

fun test4() {
    var x = AAA
    x = ++<!VARIABLE_EXPECTED!>AAA<!>
}
