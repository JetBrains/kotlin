fun box(): String {
    val result = runTest{minByTest<Int> { it }}

    if (result != 1) return "test1: ${result}"

    return "OK"
}