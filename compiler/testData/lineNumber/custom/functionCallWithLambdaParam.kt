fun foo() {
    foo({
            val a = 1
        })

    foo() {
        val a = 1
    }
}

fun foo(f: () -> Unit) {
    f()
}

// IGNORE_BACKEND: JVM_IR
// 2 6 9 12 13 3 4 7 8
