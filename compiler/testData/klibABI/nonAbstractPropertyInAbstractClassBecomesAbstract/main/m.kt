import lib1.A
import lib2.B
import lib2.B1
import lib2.B2
import lib2.B3
import lib2.B4

fun test1(): String {
    val a: A = B()
    return try {
        val answer: Int = a.foo1 // <-- should throw linkage error here
        println(answer)
        "FAIL1"
    } catch(e: Throwable) {
        e.checkLinkageError("foo1.<get-foo1>", "B")
    }
}

fun test2(): String {
    val a: A = B()
    return try {
        val answer: Int = a.foo2 // <-- should throw linkage error here
        println(answer)
        "FAIL2"
    } catch(e: Throwable) {
        e.checkLinkageError("foo2.<get-foo2>", "B")
    }
}

fun test3(): String {
    val a: A = B()
    return try {
        val answer: Int = a.bar1 // <-- should throw linkage error here
        println(answer)
        "FAIL1"
    } catch(e: Throwable) {
        e.checkLinkageError("bar1.<get-bar1>", "B")
    }
}

fun test4(): String {
    val a: A = B()
    return try {
        val answer: Int = a.bar2 // <-- should throw linkage error here
        println(answer)
        "FAIL2"
    } catch(e: Throwable) {
        e.checkLinkageError("bar2.<get-bar2>", "B")
    }
}

fun test5(): String {
    val a: A = B()
    val baz1 = a.baz1
    return if (baz1 == -42) "OK" else "baz1=$baz1"
}

fun test6(): String {
    val a: A = B()
    val baz2 = a.baz2
    return if (baz2 == -42) "OK" else "baz2=$baz2"
}

fun test7(): String {
    val b = B()
    return try {
        val answer: Int = b.unlinkedPropertyUsage // <-- should throw linkage error here
        println(answer)
        "FAIL3"
    } catch (e: Throwable) {
        e.checkLinkageError("foo1.<get-foo1>", "B")
    }
}

fun test8(): String {
    return try {
        B1()
        "FAIL4"
    } catch (e: Throwable) {
        e.checkLinkageError("foo1.<get-foo1>", "B1")
    }
}

fun test9(): String {
    return try {
        B2()
        "FAIL5"
    } catch (e: Throwable) {
        e.checkLinkageError("foo2.<get-foo2>", "B2")
    }
}

fun test10(): String {
    return try {
        B3()
        "FAIL6"
    } catch (e: Throwable) {
        e.checkLinkageError("bar1.<get-bar1>", "B3")
    }
}

fun test11(): String {
    return try {
        B4()
        "FAIL7"
    } catch (e: Throwable) {
        e.checkLinkageError("bar2.<get-bar2>", "B4")
    }
}

fun box(): String = checkResults(test1(), test2(), test3(), test4(), test5(), test6(), test7(), test8(), test9(), test10(), test11())

private fun Throwable.checkLinkageError(symbolName: String, className: String): String {
    if (this::class.simpleName != "IrLinkageError") return "Unexpected throwable: ${this::class}"

    val expectedMessage = "Abstract property accessor $symbolName is not implemented in non-abstract class $className"
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
