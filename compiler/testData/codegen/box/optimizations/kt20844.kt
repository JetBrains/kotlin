// KJS_WITH_FULL_RUNTIME
//WITH_RUNTIME

fun foo(x: String, ys: List<String>) =
        x + ys.fold("", { a, b -> a + b })

var flag = true

fun box(): String =
        foo("O", if (flag) listOf("k").map { it.toUpperCase() } else listOf())