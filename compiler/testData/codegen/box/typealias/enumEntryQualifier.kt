// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
enum class MyEnum {
    O;
    companion object {
        val K = "K"
    }
}

typealias MyAlias = MyEnum

fun box() = MyAlias.O.name + MyAlias.K
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: ENUMS
