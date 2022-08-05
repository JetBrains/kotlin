import lib1.A
import lib2.B

fun box(): String {
    val a: A = B()

    return try {
        val answer: Int = a.foo() // <-- should throw linkage error here
        println(answer)
        "FAIL"
    } catch (e: Throwable) {
        e.checkLinkageError("lib2.B.foo")
    }
}

private fun Throwable.checkLinkageError(symbolName: String): String =
    if (this::class.simpleName == "IrLinkageError"
        && message == "Abstract function $symbolName is not implemented in non-abstract class ${symbolName.substringBeforeLast(".")}"
    ) {
        "OK"
    } else
        message!!
