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

// JVM_TEMPLATES
// 1 IFNONNULL

// There should be no null checks in the bytecode.
// JVM_IR_TEMPLATES
// 0 IFNONNULL
