// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class A(
        val a: String = {
            open class B() {
                open fun s() : String = "O"
            }

            val o = object : B() {
                override fun s(): String = "K"
            }

            B().s() + o.s()
        }()
)

fun box() : String {
    return A().a
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
