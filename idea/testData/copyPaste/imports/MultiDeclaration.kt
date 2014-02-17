package a

class A() {
}

fun A.component1() = 1
fun A.component2() = 2

<selection>fun f() {
    val (a, b) = A()
}</selection>