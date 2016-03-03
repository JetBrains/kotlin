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
    ++MismatchingTypes<!NO_SET_METHOD!>[0]<!>
    MismatchingTypes<!NO_SET_METHOD!>[0]<!>++
    MismatchingTypes<!NO_SET_METHOD!>[0]<!> += 1
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
    ++MismatchingArities1<!NO_SET_METHOD!>[0]<!>
    MismatchingArities1<!NO_SET_METHOD!>[0]<!>++
    MismatchingArities1<!NO_SET_METHOD!>[0]<!> += 1

    ++<!NO_VALUE_FOR_PARAMETER!>MismatchingArities2[0]<!>
    <!NO_VALUE_FOR_PARAMETER!>MismatchingArities2[0]<!>++
    <!NO_VALUE_FOR_PARAMETER!>MismatchingArities2[0]<!> += 1
}

