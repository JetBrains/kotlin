// "Create secondary constructor" "true"

trait T

class A: T

fun test() {
    val t: T = A(<caret>1)
}