// KJS_WITH_FULL_RUNTIME
fun StringBuilder.first() = this.get(0)

fun foo() = StringBuilder("foo").first()

fun box() = if (foo() == 'f') "OK" else "Fail ${foo()}"



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_STRING_BUILDER
