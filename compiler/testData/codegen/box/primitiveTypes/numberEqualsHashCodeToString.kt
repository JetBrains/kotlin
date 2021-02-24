fun test(n: Number) {
    if (!n.equals(n)) throw Error("fail 1 for $n")
    if (n.equals(1)) throw Error("fail 2 for $n")
    if (n.equals(1L)) throw Error("fail 3 for $n")
    if (n != n) throw Error("fail 4 for $n")
    if (n == 1) throw Error("fail 5 for $n")
    if (n == 1L) throw Error("fail 6 for $n")
    if (n.hashCode() != n.toInt()) throw Error("fail 7 for $n")
    if (n.toString() != "$n") throw Error("fail 8 for $n")
}

fun box(): String {
    test(234)
    test(546L)

    return "OK"
}
