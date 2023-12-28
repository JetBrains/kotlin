// !DIAGNOSTICS: -UNUSED_PARAMETER

object Legal {
    operator fun get(i: Int) = 0
    operator fun set(i: Int, newValue: Int) {}
    operator fun set(i: Int, newValue: String) {}
}

fun testLegal() {
    ++Legal[0]
    Legal[0]++
    Legal[0] += 1
}

object MismatchingTypes {
    operator fun get(i: Int) = 0
    operator fun set(i: Int, newValue: String) {}
}

fun testMismatchingTypes() {
    <!ARGUMENT_TYPE_MISMATCH!>++MismatchingTypes[0]<!>
    <!ARGUMENT_TYPE_MISMATCH!>MismatchingTypes[0]++<!>
    MismatchingTypes[0] <!UNRESOLVED_REFERENCE!>+=<!> 1
}

object MismatchingArities1 {
    operator fun get(i: Int) = 0
    operator fun set(i: Int, j: Int, newValue: Int) {}
}

object MismatchingArities2 {
    operator fun get(i: Int, j: Int) = 0
    operator fun set(i: Int, newValue: Int) {}
}

fun testMismatchingArities() {
    <!NO_VALUE_FOR_PARAMETER!>++MismatchingArities1[0]<!>
    <!NO_VALUE_FOR_PARAMETER!>MismatchingArities1[0]++<!>
    MismatchingArities1[0] <!UNRESOLVED_REFERENCE!>+=<!> 1

    ++<!NO_VALUE_FOR_PARAMETER!>MismatchingArities2[0]<!>
    <!NO_VALUE_FOR_PARAMETER!>MismatchingArities2[0]<!>++
    <!NO_VALUE_FOR_PARAMETER!>MismatchingArities2[0]<!> += 1
}

