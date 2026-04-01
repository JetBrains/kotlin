// ISSUE: KT-83740
// FILE: lib.kt
inline fun bar(lambda: () -> String): String = lambda()

// FILE: main.kt
var magic = "42"

fun box(): String {
    val value = "OK"
    return when {
        bar { "foo" } == "bar" -> "FAIL IrReturnableBlock(IrInlinedFunctionBlock)"
        "0" == magic -> "FAIL IrCall <get-magic>()"
        value == null -> "FAIL IrGetValue"
        (if (bar { if (value == "FAIL") "A" else "B" } == "A") "A" else "B") == "A" -> "FAIL IrWhen(EQEQ(IrReturnableBlock(IrInlinedFunctionBlock(IrWhen(EQEQ(IrGetValue))))))"
        else -> value.toString()
    }
}
