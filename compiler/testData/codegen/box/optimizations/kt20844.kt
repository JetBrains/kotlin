// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_GENERATED
// WITH_STDLIB

fun foo(x: String, ys: List<String>) =
        x + ys.fold("", { a, b -> a + b })

var flag = true

fun box(): String =
        foo("O", if (flag) listOf("k").map { it.toUpperCase() } else listOf())