class A(val result: Int) {
    companion object {
        fun foo(): Int = 1
        val prop = 2
        val C = 3
    }
    object B {
        fun bar(): Int = 4
        val prop = 5
    }
    object C {
    }

    constructor() : this(foo() + prop + B.bar() + B.prop + C)
}

fun box(): String {
    val result = A().result
    if (result != 15) return "fail: $result"
    return "OK"
}
