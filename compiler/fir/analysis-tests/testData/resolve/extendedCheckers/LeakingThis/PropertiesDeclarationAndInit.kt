class A {
    val x: Int
    val useUnitialized = x +
            y
    var y: Int

    init {
        x + y  // not reporting leaking this due init block
    }
}
