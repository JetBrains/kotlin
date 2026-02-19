class A(val foo: Int, bar: Int) {
    val prop = 1

    constructor(a: Int, b: Int = <expr>a</expr>, c: Int = b): this(a + b, b + c) {
        val local = 1
    }

    fun foo() = 1
}