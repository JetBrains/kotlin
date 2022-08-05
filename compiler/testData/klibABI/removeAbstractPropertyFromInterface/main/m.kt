import lib1.A
import lib2.B

fun box(): String {
    val a: A = B()

    return try {
        val answer: Int = a.foo // <-- should throw linkage error here
        println(answer)
        "FAIL"
    } catch (e: Throwable) {
        e.checkLinkageError("lib2.B.foo.<get-foo>", "lib2.B")
    }
}

private fun Throwable.checkLinkageError(symbolName: String, className: String): String =
    if (this::class.simpleName == "IrLinkageError"
            && message == "Abstract property accessor $symbolName is not implemented in non-abstract class $className"
    ) {
        "OK"
    } else
        message!!
