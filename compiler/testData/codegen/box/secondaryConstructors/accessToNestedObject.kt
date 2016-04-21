class A(val result: Int) {
    object B {
        fun bar(): Int = 4
        val prop = 5
    }
    object C {
    }

    constructor() : this(B.bar() + B.prop)
}

fun box(): String {
    val result = A().result
    if (result != 9) return "fail: $result"
    return "OK"
}
