// LANGUAGE: -IrInlinerBeforeKlibSerialization
// SKIP_UNBOUND_IR_SERIALIZATION
// Should be fixed in WASM as side effect of KT-74392
// When fixed, please remove LANGUAGE and SKIP_UNBOUND_IR_SERIALIZATION directives and remove the test `kt67024WithInlinedFunInKlib.kt`
// IGNORE_BACKEND: WASM

inline fun <reified T> id(x: T) = x

fun test1(block: (String) -> String = ::id)  = block("O")
inline fun test2(block: (String) -> String = ::id)  = block("K")

fun box() : String {
    return test1() + test2()
}
