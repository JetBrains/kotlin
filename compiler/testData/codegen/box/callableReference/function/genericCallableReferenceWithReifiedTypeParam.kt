// WITH_STDLIB

import kotlin.test.assertEquals


inline fun <reified T> funNoArgs() = "OK" as? T

fun testFunctionNoArgs() {
    val callable: () -> String? = ::funNoArgs
    assertEquals(callable(), "OK")
}

inline fun <reified T> funWithArgs(x: T, y: T) = x to y

fun testFunctionWithArgs() {
    val callable: (String, String) -> Pair<String, String> = ::funWithArgs
    assertEquals(callable("O", "K"), "O" to "K")
}

inline fun <reified T> funWithVarargs(vararg i: T) = i.toList()

fun testFunctionWithVarargs() {
    val callable: (Array<Int>) -> List<Int> = ::funWithVarargs
    assertEquals(callable(arrayOf(1, 2, 3)), listOf(1, 2, 3))
}

inline fun <reified T> T.funWithExtensionNoArgs() = this

fun testFunctionWithExtensionNoArgs() {
    val callable1 = String::funWithExtensionNoArgs
    assertEquals(callable1("OK1"), "OK1")

    val callable2 = "OK2"::funWithExtensionNoArgs
    assertEquals(callable2(), "OK2")

    val callable3 = callable1::funWithExtensionNoArgs
    assertEquals(callable3()("OK3"), "OK3")

    val callable4 = with("OK4") { ::funWithExtensionNoArgs }
    assertEquals(callable4(), "OK4")
}

inline fun <reified T> T.funWithExtensionAndArgs(x: Int, y: Int) = this to (x + y)

fun testFunctionWithExtensionAndArgs() {
    val callable1 = String::funWithExtensionAndArgs
    assertEquals(callable1("OK1", 1, 2), "OK1" to 3)

    val callable2 = "OK2"::funWithExtensionAndArgs
    assertEquals(callable2(3, 4), "OK2" to 7)

    val callable3 = callable1::funWithExtensionAndArgs
    val (cb, s) = callable3(5, 6)
    assertEquals(s, 11)
    assertEquals(cb("OK3", 7, 8), "OK3" to 15)

    val callable4 = with("OK4") { ::funWithExtensionAndArgs }
    assertEquals(callable4(9, 10), "OK4" to 19)
}

inline fun <reified T> T.funWithExtensionAndVarargs(vararg i: Int) = this to i.sum()

fun testFunctionWithExtensionAndVararg() {
    val callable1 = String::funWithExtensionAndVarargs
    assertEquals(callable1("OK1", arrayOf(1, 2, 3).toIntArray()), "OK1" to 6)

    val callable2 = "OK2"::funWithExtensionAndVarargs
    assertEquals(callable2(arrayOf(4, 5, 6).toIntArray()), "OK2" to 15)

    val callable3 = callable1::funWithExtensionAndVarargs
    val (cb, s) = callable3(arrayOf(7, 8).toIntArray())
    assertEquals(s, 15)
    assertEquals(cb("OK3", arrayOf(9, 10).toIntArray()), "OK3" to 19)

    val callable4 = with("OK4") { ::funWithExtensionAndVarargs }
    assertEquals(callable4(arrayOf(11, 12).toIntArray()), "OK4" to 23)
}

class TestClass(val s: String) {
    inline fun <reified T> classFunNoArgs() = s as? T
    inline fun <reified T> classFunWithArgs(x: T) = x to s
    inline fun <reified T> classFunWithVarargs(vararg i: T) = i.toList() to s
}

fun testClassFunctionNoArgs() {
    val callable: () -> String? = with(TestClass("OK1")) { ::classFunNoArgs }
    assertEquals(callable(), "OK1")
}

fun testClassFunctionWithArgs() {
    val callable: (Int) -> Pair<Int, String> = with(TestClass("OK1")) { ::classFunWithArgs }
    assertEquals(callable(1), 1 to "OK1")
}

fun testClassFunctionWithVarargs() {
    val callable: (Array<Int>) -> Pair<List<Int>, String> = with(TestClass("OK1")) { ::classFunWithVarargs }
    assertEquals(callable(arrayOf(1, 2)), listOf(1, 2) to "OK1")
}

fun box(): String {
    testFunctionNoArgs()
    testFunctionWithArgs()
    testFunctionWithVarargs()

    testFunctionWithExtensionNoArgs()
    testFunctionWithExtensionAndArgs()
    testFunctionWithExtensionAndVararg()

    testClassFunctionNoArgs()
    testClassFunctionWithArgs()
    testClassFunctionWithVarargs()

    return "OK"
}
