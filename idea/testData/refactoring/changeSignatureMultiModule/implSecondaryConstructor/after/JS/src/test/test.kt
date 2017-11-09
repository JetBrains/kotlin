package test

actual class C(s: String) {
    constructor(n: Int, b: Boolean): this("")
}

fun test() {
    C("1")
    C(1, false)
}