fun box(): String {
    fun a(a: Any) = a === 1.1 is Double
    return if (a(true)) "OK" else "Fail"
}
