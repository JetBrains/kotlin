fun box(): String {
    val x = object : B {}
    return x.foo()
}