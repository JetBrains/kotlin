import lib1.A
import lib2.B
import lib2.B1
import lib2.B2

fun test1(): String {
    val a: A = B()
    return try {
        val answer: Int = a.foo() // <-- should throw linkage error here
        println(answer)
        "FAIL1"
    } catch (e: Throwable) {
        e.checkLinkageError("foo", "B")
    }
}

fun test2(): String {
    val a: A = B()
    return try {
        val answer: Int = a.bar() // <-- should throw linkage error here
        println(answer)
        "FAIL2"
    } catch (e: Throwable) {
        e.checkLinkageError("bar", "B")
    }
}

fun test3(): String {
    val a: A = B()
    val baz = a.baz()
    return if (baz == -42) "OK" else "baz=$baz"
}

fun test4(): String {
    val b = B()
    return try {
        val answer: Int = b.unlinkedFunctionUsage // <-- should throw linkage error here
        println(answer)
        "FAIL3"
    } catch (e: Throwable) {
        e.checkLinkageError("foo", "B")
    }
}

fun test5(): String {
    return try {
        B1()
        "FAIL4"
    } catch (e: Throwable) {
        e.checkLinkageError("foo", "B1")
    }
}

fun test6(): String {
    return try {
        B2()
        "FAIL5"
    } catch (e: Throwable) {
        e.checkLinkageError("bar", "B2")
    }
}

fun box(): String = checkResults(test1(), test2(), test3(), test4(), test5(), test6())

private fun Throwable.checkLinkageError(symbolName: String, className: String): String {
    if (this::class.simpleName != "IrLinkageError") return "Unexpected throwable: ${this::class}"

    val expectedMessage = "Abstract function $symbolName is not implemented in non-abstract class $className"
    val actualMessage = message.orEmpty()

    return if (expectedMessage == actualMessage)
        "OK"
    else
        "EXPECTED: $expectedMessage, ACTUAL: $actualMessage"
}

private fun checkResults(vararg results: String): String = when {
    results.isEmpty() -> "no results to check"
    results.all { it == "OK" } -> "OK"
    else -> results.joinToString("\n")
}
