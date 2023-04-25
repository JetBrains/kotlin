class A(val prop: Int = 42, c: String) {
    constructor(st<caret>r: String) : this(str.myToInt(), str)

    fun foo() = "str"
}

fun String.myToInt(): Int = 42