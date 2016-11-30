suspend fun unit1() {
    <!SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE!>unit1()<!>
}

suspend fun unit2() {
    return unit2()
}

suspend fun int1(): Int {
    return int1()
}

suspend fun int2(): Int = int2()

suspend fun int3(): Int {
    <!SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE!>int3()<!>
    return int3()
}
