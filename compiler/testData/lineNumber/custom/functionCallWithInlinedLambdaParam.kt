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

// 2 17 18 3 4 19 6 20 21 7 8 22 9 12 13 14