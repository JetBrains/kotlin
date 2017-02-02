fun box(): String {
    var s1 = (l1@ "s")
    val s2 = (l2@ if (l3@ true) s1 else null)
    return if (s2 == "s") "OK" else "fail"
}
