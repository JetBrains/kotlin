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
    val p = Plus
    val pa = PlusAssign
    val ppa = PlusAndPlusAssign
}

fun ban(b: B?) {
    b?.<!VAL_REASSIGNMENT!>p<!> += 10
    (b?.p) <!NONE_APPLICABLE!>+=<!> 10

    b?.pa += 10
    (b?.pa) <!UNSAFE_OPERATOR_CALL!>+=<!> 10

    b?.ppa += 10
    (b?.ppa) <!UNSAFE_OPERATOR_CALL!>+=<!> 10
}

object PlusExt
operator fun PlusExt?.plus(number: Int) = this.also { println("p-ext -> plus") }

object PlusAssignExt
operator fun PlusAssignExt?.plusAssign(number: Int) { println("pa-ext -> plusAssign") }

object PlusAndPlusAssignExt
operator fun PlusAndPlusAssignExt?.plus(number: Int) = this.also { println("ppa-ext -> plus") }
operator fun PlusAndPlusAssignExt?.plusAssign(number: Int) { println("ppa-ext -> plusAssign") }

object C {
    val p = PlusExt
    val pa = PlusAssignExt
    val ppa = PlusAndPlusAssignExt
}

fun bad(c: C?) {
    <!ASSIGNMENT_TYPE_MISMATCH!>c?.<!VAL_REASSIGNMENT!>p<!> += 10<!>
    (c?.p) <!NONE_APPLICABLE!>+=<!> 10

    c?.pa += 10
    (c?.pa) += 10

    c?.ppa += 10
    (c?.ppa) += 10
}
