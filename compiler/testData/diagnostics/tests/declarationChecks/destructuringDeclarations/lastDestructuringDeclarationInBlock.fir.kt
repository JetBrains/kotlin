// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_PARAMETER

fun test(list: A) {
    if (true) {
        val (c) = list
    }
    else {}

    if (true) {
        Unit
        val (c) = list
    }
    else {}

    when (1) {
        1 -> {
            val (c) = list
        }
    }

    fn { it ->
        val (a) = it
    }
}

class A {
    operator fun component1() = 1
}

fun fn(x: (A) -> Unit) {}