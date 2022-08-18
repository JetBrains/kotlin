fun test(s: String) {
    if (!s.equals(s)) throw Error("fail 1 for $s")
    if (s.equals(1)) throw Error("fail 2 for $s")
    if (s != s) throw Error("fail 4 for $s")
    if (s == "111") throw Error("fail 5 for $s")
    if (s.hashCode() != -1268878963) throw Error("fail 7 for $s")
    if (s.toString() != "$s") throw Error("fail 8 for $s")
}

fun box(): String {
    test("foobar")
    return "OK"
}
