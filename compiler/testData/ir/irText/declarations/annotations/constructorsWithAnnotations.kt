// FIR_IDENTICAL
annotation class TestAnn(val x: Int)

class TestClass @TestAnn(1) constructor() {
    @TestAnn(2) constructor(x: Int) : this()
}