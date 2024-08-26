// ISSUE: KT-70507
// DIAGNOSTICS: -VARIABLE_WITH_REDUNDANT_INITIALIZER
// WITH_STDLIB
// FIR_DUMP

object Plus {
    operator fun plus(number: Int) = this.also { println("p -> plus") }
}

object PlusAssign {
    operator fun plusAssign(number: Int) { println("pa -> plusAssign") }
}

object PlusAndPlusAssign {
    operator fun plus(number: Int) = this.also { println("ppa -> plus") }
    operator fun plusAssign(number: Int) { println("ppa -> plusAssign") }
}

object B {
    var p = arrayOf(Plus)
    var pa = arrayOf(PlusAssign)
    var ppa = arrayOf(PlusAndPlusAssign)
}

fun ban(b: B?) {
    <!UNSAFE_CALL!>b?.p<!NO_SET_METHOD!>[0]<!><!> += 10
    (<!UNSAFE_CALL!>b?.p<!NO_SET_METHOD!>[0]<!><!>) += 10

    <!UNSAFE_CALL!>b?.pa[0]<!> += 10
    (<!UNSAFE_CALL!>b?.pa[0]<!>) += 10

    <!UNSAFE_CALL!>b?.ppa[0]<!> <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> 10
    (<!UNSAFE_CALL!>b?.ppa[0]<!>) <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> 10
}

object PlusExt
operator fun PlusExt?.plus(number: Int) = this.also { println("p-ext -> plus") }

object PlusAssignExt
operator fun PlusAssignExt?.plusAssign(number: Int) { println("pa-ext -> plusAssign") }

object PlusAndPlusAssignExt
operator fun PlusAndPlusAssignExt?.plus(number: Int) = this.also { println("ppa-ext -> plus") }
operator fun PlusAndPlusAssignExt?.plusAssign(number: Int) { println("ppa-ext -> plusAssign") }

object C {
    var p = arrayOf(PlusExt)
    var pa = arrayOf(PlusAssignExt)
    var ppa = arrayOf(PlusAndPlusAssignExt)
}

fun bad(c: C?) {
    <!TYPE_MISMATCH!><!UNSAFE_CALL!>c?.p<!NO_SET_METHOD!>[0]<!><!> += 10<!>
    <!TYPE_MISMATCH!>(<!UNSAFE_CALL!>c?.p<!NO_SET_METHOD!>[0]<!><!>) += 10<!>

    <!UNSAFE_CALL!>c?.pa[0]<!> += 10
    (<!UNSAFE_CALL!>c?.pa[0]<!>) += 10

    <!UNSAFE_CALL!>c?.ppa[0]<!> += 10
    (<!UNSAFE_CALL!>c?.ppa[0]<!>) += 10
}
