package test

interface  A {
    fun run()
}

inline fun testNested(crossinline f: (String) -> Unit) {
    object : A {
        override fun run() {
            f("OK")
        }
    }.run()
}

fun test(f: (String) -> Unit) {
    testNested { it ->  { f(it) }()}
}

