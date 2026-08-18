// FILE: Delegation.java
package test;

class JavaClass {
    void testMethod() {
        Test test = new Test();
        test.none();
        test.one();
        test.two();
    }
}

// FILE: Delegation.kt
package test

class E1: Exception()
class E2: Exception()

interface Trait {
    @Throws()
    fun none()

    @Throws(E1::class)
    fun one()

    @Throws(E1::class, E2::class)
    fun two()
}

class Impl: Trait {
    override fun none() {}
    override fun one() {}
    override fun two() {}
}

class Test: Trait by Impl()
