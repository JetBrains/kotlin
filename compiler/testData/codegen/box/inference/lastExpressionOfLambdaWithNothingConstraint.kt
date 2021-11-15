// WITH_STDLIB

inline fun <T> foo(f: () -> T): String {
    return (f() as? Inv<T>)?.result() ?: "Bad"
}

class Inv<T> {
    fun result(): String = "OK"
}

fun <K> create(): Inv<K> = Inv()

fun test(b: Boolean): String {
    return foo {
        if (!b) {
            return@foo create<String>()
        }

        if (b) {
            create<String>()
        } else {
            null
        }
    }
}

fun box(): String {
    return test(true)
}
