// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR

@Target(AnnotationTarget.PROPERTY)
annotation class Anno

inline class Z(val s: String)

class A {
    @Anno
    val Z.r: String get() = s
}

fun box(): String {
    with(A()) {
        return Z("OK").r
    }
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ with 
