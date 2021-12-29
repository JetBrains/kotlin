// KT-27427

interface A {
    fun foo()
}

class B : A {
    override fun foo() {
    }
}

fun test1() {
    val b = B()
    (b as A).foo()
}

fun test2() {
    val b = getB()
    (b as A).foo()
}

fun test3() {
    val b = getB()
    b.foo()
}

fun getB(): B = B()

// 1 IFNONNULL
