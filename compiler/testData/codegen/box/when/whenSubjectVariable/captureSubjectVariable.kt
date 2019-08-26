// !LANGUAGE: +VariableDeclarationInWhenSubject
// IGNORE_BACKEND: WASM

fun box(): String {
    var y: String = "OK"

    var materializer: (() -> String)? = null

    when (val x = y) {
        "OK" -> materializer = { x }
        else -> return "x is $x"
    }

    y = "Fail"

    return materializer!!.invoke()
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
