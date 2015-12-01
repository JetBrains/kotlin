fun foo() {
    foo({
            val a = 1
        })

    foo() {
        val a = 1
    }
}

inline fun foo(f: () -> Unit) {
    val a = 1
    f()
}

// 2 12 3 4 6 12 7 8 9 12 13 14