fun some(f: (String) -> Unit) {}

fun test() {
    3 + object {
        fun foo() {

        }
    }

    2 ?: some {
        s ->
        val a = 12
    }
}

// SET_TRUE: ALIGN_MULTILINE_BINARY_OPERATION