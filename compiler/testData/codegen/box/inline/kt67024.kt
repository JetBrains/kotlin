// Should be fixed in WASM as side effect of KT-74392
// IGNORE_BACKEND: WASM
// IGNORE_NATIVE: compatibilityTestMode=NewArtifactOldCompiler
// ^^^ This new test fails in 2.1.0 compiler backend and passes on 2.2.0 and later


inline fun <reified T> id(x: T) = x

fun test1(block: (String) -> String = ::id)  = block("O")
inline fun test2(block: (String) -> String = ::id)  = block("K")

fun box() : String {
    return test1() + test2()
}
