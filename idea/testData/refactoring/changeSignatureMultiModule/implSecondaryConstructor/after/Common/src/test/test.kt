package test

header class C(s: String) {
    constructor(n: Int, b: Boolean): this("")
}

fun test() {
    C(1, false)
    C("1")
}