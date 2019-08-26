// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

// FILE: test.kt

fun foo() {
    takeClass(run {
        val outer: Sample<out Any>? = null
        if (outer != null) outer else null
    })
}

fun takeClass(instanceClass: Sample<*>?) {}
class Sample<T : Any>

fun box(): String {
    foo()
    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ run 
