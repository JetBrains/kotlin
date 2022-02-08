// WITH_STDLIB

interface T {
}

fun box(): String {
    val a = "OK"
    val t = object : T {
        val foo by lazy {
            a
        }
    }
    return t.foo
}
