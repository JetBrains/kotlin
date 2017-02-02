fun foo(b: Boolean) =
    when (b) {
        false -> 0
        true -> 1
        else -> 2
    }

fun box(): String = if (foo(false) == 0 && foo(true) == 1) "OK" else "Fail"

