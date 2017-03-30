// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

protocol interface X {
    fun foo()
}

class A {
    fun foo() {}
}

class B {
    fun foo() {}
}

class Y {
    val data = mutableListOf<X>()

    fun test() {
        for (x in data) {
            x.foo()
        }
    }
}

fun box(): String {
    val y = Y()
    y.test()

    return "OK"
}