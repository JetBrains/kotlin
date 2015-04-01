class K : J()

fun box(): String {
    val k = K()
    val p = K::result
    if (p.get(k) != null) return "Fail"
    p.set(k, "OK")
    return p.get(k)
}
