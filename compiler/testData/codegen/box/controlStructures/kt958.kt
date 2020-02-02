// IGNORE_BACKEND: WASM
fun test() = 239

fun box() = if(test() in 239..240) "OK" else "fail"
