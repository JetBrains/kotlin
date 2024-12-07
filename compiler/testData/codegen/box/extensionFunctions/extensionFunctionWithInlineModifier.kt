inline fun foo(noinline a: Int.()-> Unit) {
    a
}

inline fun bar(crossinline a: Int.()-> Unit) {
    val s = object {
        fun run() {
            a(1)
        }
    }
    s.run()
}

inline fun baz(a: Int.()-> Unit) {
    a(1)
}

fun box(): String {
    foo { }
    bar { }
    baz {
        return "OK"
    }
    return "FAIL"
}