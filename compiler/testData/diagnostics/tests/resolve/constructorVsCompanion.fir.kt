class A private constructor()

class B {
    private companion object
}

class C(val x: Int)

class D private constructor() {
    companion object
}

class E private constructor() {
    companion object {
        operator fun invoke(x: Int) = x
    }
}

val a = A
val b = B
val c = C
val d = D
val e = E(42)
