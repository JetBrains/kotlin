var log = ""

fun doTest(id: String, expected: Int, expectedLog: String, test: () -> Int) {
    log = ""
    val actual = test()
    if (actual != expected) throw AssertionError("$id expected: $expected, actual: $actual")
    if (log != expectedLog) throw AssertionError("$id expectedLog: $expectedLog, actual: $log")
}

var x = 0
    get() = field.also { log += "get;" }
    set(value: Int) {
        log += "set;"
        field = value
    }

fun box(): String {
    // NOTE: Getter is currently called twice for prefix increment; 1st for initial value, 2nd for return value. See KT-42077.
    doTest("++x", 1, "get;set;get;") { ++x }
    doTest("x++", 1, "get;set;") { x++ }
    doTest("x--", 2, "get;set;") { x-- }
    doTest("--x", 0, "get;set;get;") { --x }

    return "OK"
}
