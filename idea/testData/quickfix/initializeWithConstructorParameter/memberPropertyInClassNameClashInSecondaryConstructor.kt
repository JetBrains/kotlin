// "Initialize with constructor parameter" "true"
open class A {
    <caret>val n: Int

    constructor(n: Int)
}

class B : A(1)

fun test() {
    val a = A(1)
}