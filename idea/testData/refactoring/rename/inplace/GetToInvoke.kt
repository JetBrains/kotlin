class A {
    operator fun <caret>get(n: Int, s: String) = 1
}

fun test() {
    A()[1, "2"]
}