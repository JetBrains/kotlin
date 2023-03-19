// IGNORE_FE10
class A {
    operator fun <T> set(key: Int, value: T) {}
}

fun test(a: A) {
    <expr>a[1]</expr> = ""
}