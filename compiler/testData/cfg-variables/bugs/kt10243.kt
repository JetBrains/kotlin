var x: Int
fun foo(f: Boolean) {
    try {
        if (f) {
            x = 0
        }
    }
    finally {
        fun bar() {}
    }
}