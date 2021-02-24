fun foo(x: String) = x

fun test1() {
    var c: Any? = "XXX"
    if (c !is String) return

    val newC: String? = "YYY"
    if (newC != null) {
        c = newC
    }
    foo(c)
}

fun test2() {
    var c: Any? = "XXX"
    if (c !is String) return

    val newC: String? = "YYY"
    if (newC is String) {
        c = newC
    }
    foo(c)
}

fun test3() {
    var c: Any? = "XXX"
    if (c !is String) return

    val newC: String? = "YYY"
    if (newC == null) return
    c = newC

    foo(c)
}

