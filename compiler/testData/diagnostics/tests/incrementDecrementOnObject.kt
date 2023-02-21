// SKIP_TXT
// ISSUE: KT-56659

object AAA { operator fun inc(): AAA = this }

fun test1() {
    <!VAL_REASSIGNMENT!>AAA<!>++
}

fun test2() {
    ++<!VAL_REASSIGNMENT!>AAA<!>
}

fun test3() {
    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>x<!> = AAA
    x = <!VAL_REASSIGNMENT!>AAA<!>++
}

fun test4() {
    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>x<!> = AAA
    x = ++<!VAL_REASSIGNMENT!>AAA<!>
}
