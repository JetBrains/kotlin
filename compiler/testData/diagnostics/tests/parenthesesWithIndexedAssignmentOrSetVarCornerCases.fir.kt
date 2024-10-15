// RUN_PIPELINE_TILL: FRONTEND
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
    b?.p[0] += 10
    (b?.p[0]) <!NONE_APPLICABLE!>+=<!> 10

    b?.pa[0] += 10
    (b?.pa[0]) <!UNSAFE_OPERATOR_CALL!>+=<!> 10

    b?.ppa[0] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> 10
    (b?.ppa[0]) <!UNSAFE_OPERATOR_CALL!>+=<!> 10
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
    c?.p[0] <!UNRESOLVED_REFERENCE!>+=<!> 10
    (c?.p[0]) <!NONE_APPLICABLE!>+=<!> 10

    c?.pa[0] += 10
    (c?.pa[0]) += 10

    c?.ppa[0] += 10
    (c?.ppa[0]) += 10
}
