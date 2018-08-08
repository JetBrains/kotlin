fun box(): String {
    val o = object {
        inner class A(val value: String = "OK")
    }

    return o.A().value
}