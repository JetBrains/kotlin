// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: CALLABLE_REFERENCES_FAIL
fun <T> id(x: T): T = x
fun <T> String.extId(x: T): T = x

fun <T, R> T.myLet(block: (T) -> R): R = block(this)

fun <T> foo(value: T?): T? = value?.myLet(::id) // KFunction1<Nothing, T>
fun <T> bar(value: T?): T? = value?.myLet(""::extId)

fun box() = foo("O")!! + foo("K")!!
