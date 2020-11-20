// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: SPREAD_OPERATOR
// !LANGUAGE: +NewInference +MixedNamedArgumentsInTheirOwnPosition
// IGNORE_BACKEND: JVM
// See KT-17691: Wrong argument order in resolved call with varargs. (fixed in JVM_IR)

var log = ""

fun <T> logged(value: T): T =
    value.also { log += " " + value }

fun doTest(id: String, expected: String, expectedLog: String, test: () -> String) {
    log = ""
    val actual = test()
    if (actual != expected) throw AssertionError("$id expected: $expected, actual: $actual")
    if (log != expectedLog) throw AssertionError("$id expectedLog: $expectedLog, actual: $log")
}

fun foo1(
    vararg p1: Int,
    p2: String,
    p3: Double
) = "${p1[0]} ${p1[1]} $p2 ${p3.toInt()}"

fun foo2(
    p1: Int,
    vararg p2: String,
    p3: Double
) = "$p1 ${p2[0]} ${p2[1]} ${p3.toInt()}"

fun foo3(
    p1: Int,
    p2: String,
    vararg p3: Double
) = "$p1 $p2 ${p3[0].toInt()} ${p3[1].toInt()}"

fun box(): String {
    doTest("test1", "1 2 3 4", " 1 2 3 4.5") { foo1(logged(1), logged(2), p2 = logged("3"), p3 = logged(4.5)) }
    doTest("test2", "1 2 3 4", " 1 2 3 4.5") { foo1(p1 = *intArrayOf(logged(1), logged(2)), logged("3"), p3 = logged(4.5)) }

    doTest("test3", "1 2 3 4", " 1 2 3 4.5") { foo2(p1 = logged(1), logged("2"), logged("3"), p3 = logged(4.5)) }
    doTest("test4", "1 2 3 4", " 1 2 3 4.5") { foo2(logged(1), p2 = *arrayOf(logged("2"), logged("3")), logged(4.5)) }

    doTest("test5", "1 2 3 4", " 1 2 3.5 4.5") { foo3(p1 = logged(1), logged("2"), logged(3.5), logged(4.5)) }
    doTest("test6", "1 2 3 4", " 1 2 3.5 4.5") { foo3(p1 = logged(1), logged("2"), p3 = *doubleArrayOf(logged(3.5), logged(4.5))) }

    return "OK"
}
