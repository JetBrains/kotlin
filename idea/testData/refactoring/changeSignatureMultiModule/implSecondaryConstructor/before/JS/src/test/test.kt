package test

actual class C(s: String) {
    actual <caret>constructor(n: Int): this("")
}

fun test() {
    C("1")
    C(1)
}