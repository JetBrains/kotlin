class Test1<T1, T2>(val x: T1, val y: T2)

class Test2(x: Int, val y: String) {
    inner class TestInner<Z>(val z : Z) {
        constructor(z: Z, i: Int) : this(z)
    }
}

class Test3(val x: Int, val y: String = "")

class Test4<T>(val x: Int) {
    constructor(x: Int, y: Int = 42) : this(x + y)
}
