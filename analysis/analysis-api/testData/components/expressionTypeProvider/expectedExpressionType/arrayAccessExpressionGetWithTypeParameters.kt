// IGNORE_FE10
class A {
    operator fun <T> get(key: T) {}
}

fun test(a: A) {
    a[a<caret>v]
}