class A() {
    operator fun p<caret>lus(rhs: A): A {
        return A()
    }
}

val a1 = A()
val a2 = A()
val a3 = a1 + a2
val a4 = a1.plus(a2)
val a5 = a1 plus a2

