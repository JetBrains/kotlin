fun box(): String {
    val o = object {
        class A(val value: String = "OK")
    }

    return o.A().value
}