// LANGUAGE: -IrInlinerBeforeKlibSerialization
// SKIP_UNBOUND_IR_SERIALIZATION
// Should be fixed in WASM as side effect of KT-74392
// When fixed, please remove LANGUAGE and SKIP_UNBOUND_IR_SERIALIZATION directives and remove the test `kt69041WithInlinedFunInKlib.kt`
// IGNORE_BACKEND: WASM

class A {
    inline fun <reified T> foo(x: T) = x
}

fun test(block: (A, String) -> String) = block(A(), "OK")

fun box() : String {
    return test(A::foo)
}
