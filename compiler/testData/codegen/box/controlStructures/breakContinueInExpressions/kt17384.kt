fun returnNullable(): String? = null

inline fun Array<String>.matchAll(fn: (String) -> Unit) {
    for (string in this) {
        fn(returnNullable() ?: continue)
    }
}

fun Array<String>.matchAll2(fn: (String) -> Unit) {
    matchAll(fn)
}

inline fun Array<String>.matchAll3(crossinline fn: (String) -> Unit) {
    matchAll2 { fn(it) }
}

fun test(a: Array<String>) {
    a.matchAll {}
    a.matchAll2 {}
    a.matchAll3 {}
}

fun box(): String {
    test(arrayOf(""))
    return "OK"
}