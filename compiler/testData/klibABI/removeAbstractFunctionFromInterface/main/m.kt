import lib1.A
import lib2.B

fun box(): String {
    val a: A = B()

    return try {
        val answer: Int = a.foo() // <-- should throw linkage error here
        println(answer)
        "FAIL"
    } catch (e: Throwable) {
        e.checkLinkageError("foo", "B")
    }
}

private fun Throwable.checkLinkageError(symbolName: String, className: String): String {
    if (this::class.simpleName != "IrLinkageError") return "Unexpected throwable: ${this::class}"

    val expectedMessage = "Abstract function $symbolName is not implemented in non-abstract class $className"
    val actualMessage = message.orEmpty()

    return if (expectedMessage == actualMessage)
        "OK"
    else
        "EXPECTED: $expectedMessage, ACTUAL: $actualMessage"
}
