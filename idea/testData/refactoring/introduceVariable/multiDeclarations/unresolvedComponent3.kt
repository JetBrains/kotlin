class A

operator fun A.component1() = 1
operator fun A.component2() = 2

class B {
    operator fun A.component3() = 3
}

fun test() {
    <selection>A()</selection>
}