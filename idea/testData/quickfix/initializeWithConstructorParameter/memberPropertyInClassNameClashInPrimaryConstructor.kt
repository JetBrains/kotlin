// "Initialize with constructor parameter" "true"
open class A(n: Int) {
    <caret>var n: Int
        get() = 1
}

class B : A(0)

fun test() {
    val a = A(0)
}