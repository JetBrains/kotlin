// !LANGUAGE: -ProhibitComparisonOfIncompatibleEnums
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR

enum class A {
    OK
}

enum class B {
    FAIL
}

fun f() = A.OK

fun box(): String {
    return when (f()) {
        B.FAIL -> "fail"
        A.OK -> "OK"
    }
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: ENUMS
