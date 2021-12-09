open class A {
    fun foo(i: Int = 42) {}
}

class B : A()

fun test() {
    B().foo()
}

// JVM_IR_TEMPLATES
// 0 CHECKCAST
