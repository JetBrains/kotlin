package test

actual class C actual constructor(n: Int) {
    constructor(s: String): this(1)
}

fun test() {
    C("1")
    C(1)
}