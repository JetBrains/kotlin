// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

open class Base(val bar: String)

class Foo(bar: String) : Base(bar) {
  fun something() = bar.toUpperCase()
}

fun box() = Foo("ok").something()

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ toUpperCase 
