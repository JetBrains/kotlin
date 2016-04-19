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

// 2 18 19 3 4 20 6 21 22 7 8 23 9 12 13 14
