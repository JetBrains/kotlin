class A(val a: Int, val b: Int) {
    val foo: Int get() = { <selection>a + b</selection> - 1 }.invoke()
}