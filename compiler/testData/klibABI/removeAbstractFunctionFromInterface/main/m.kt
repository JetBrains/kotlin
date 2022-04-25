import lib1.A
import lib2.B

fun box(): String {
    val a: A = B()

    try {
        val answer: Int = a.foo() // <-- should throw linkage error here
        println(answer)
    } catch (e: Throwable) {
        if (e.isLinkageError("lib2.B.foo")) return "OK"
    }

    return "FAIL"
}

private fun Throwable.isLinkageError(symbolName: String): Boolean =
    this::class.simpleName == "IrLinkageError"
            && message == "Abstract function $symbolName is not implemented in non-abstract class ${symbolName.substringBeforeLast(".")}"
