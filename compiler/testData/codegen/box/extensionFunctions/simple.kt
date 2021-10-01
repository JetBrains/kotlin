// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_TEXT
// KJS_WITH_FULL_RUNTIME
fun StringBuilder.first() = this.get(0)

fun foo() = StringBuilder("foo").first()

fun box() = if (foo() == 'f') "OK" else "Fail ${foo()}"
