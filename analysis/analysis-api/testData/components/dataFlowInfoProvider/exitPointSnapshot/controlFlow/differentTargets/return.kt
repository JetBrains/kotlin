fun test(flag: Boolean): Int {
    block {
        <expr>if (flag) {
            return 1
        }

        consume("foo")
        return@block</expr>
    }

    return 0
}

inline fun block(block: () -> Unit) {}

fun consume(obj: Any?) {}