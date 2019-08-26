// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

class Environment(
    val fieldAccessedInsideChild: Int,
    val how: Environment.() -> Unit
)
fun box(): String {
    Environment(
        3,
        {
            class Child {
                val a = fieldAccessedInsideChild
            }
            class Parent {
                val children: List<Child> =
                    (0..4).map { Child() }
            }
        }
    )

    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ .. 
