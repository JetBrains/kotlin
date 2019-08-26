// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
var result = ""
fun result(r: String) { result = r }

object Foo {
    private operator fun String.unaryPlus() = "(" + this + ")"

    fun foo() = { result(+"Stuff") }()
}

fun box(): String {
    Foo.foo()
    return if (result == "(Stuff)") "OK" else "Fail $result"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
