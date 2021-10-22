fun box(): String {
    return object {
        val a = A("OK")
        inner class A(val ok: String)
    }.a.ok
}
