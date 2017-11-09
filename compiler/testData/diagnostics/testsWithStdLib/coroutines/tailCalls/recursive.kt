suspend fun unit1() {
    unit1()
}

suspend fun unit2() {
    return unit2()
}

suspend fun int1(): Int {
    return int1()
}

suspend fun int2(): Int = int2()

suspend fun int3(): Int {
    int3()
    return int3()
}
