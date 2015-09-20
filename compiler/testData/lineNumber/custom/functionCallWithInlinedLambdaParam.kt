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

// 2 12 13 3 14 6 12 13 7 14 9 12 13 14
