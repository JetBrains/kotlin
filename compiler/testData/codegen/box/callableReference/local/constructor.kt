fun box(): String {
    class A {
        val result = "OK"
    }

    return (::A)().result
}
