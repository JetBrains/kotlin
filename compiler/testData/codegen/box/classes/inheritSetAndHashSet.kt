// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: NATIVE

interface A : Set<String>

class B : A, HashSet<String>()

fun box(): String {
    val b = B()
    b.add("OK")
    return b.iterator().next()
}



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_HASH_SET
