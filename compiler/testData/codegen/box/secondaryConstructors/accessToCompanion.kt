class A(val result: Int) {
    companion object {
        fun foo(): Int = 1
        val prop = 2
        val C = 3
    }
    object C {
    }

    constructor() : this(foo() + prop + C)
}

fun box(): String {
    val result = A().result
    if (result != 6) return "fail: $result"
    return "OK"
}
