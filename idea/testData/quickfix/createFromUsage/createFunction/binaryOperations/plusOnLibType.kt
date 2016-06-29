// "Create extension function 'Int.plus'" "true"
// WITH_RUNTIME

class A<T>(val n: T)

fun test() {
    val a: A<Int> = 2 <caret>+ A(1)
}