package test

header class C(s: String) {
    constructor(n: Int): this("")
}

fun test() {
    C(1)
    C("1")
}