fun box(): String {
    class A {
        val result = "OK"
    }

    return (::A).let { it() }.result
}
