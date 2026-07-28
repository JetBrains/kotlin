// FILE: ManyClassesHierarchy.java
package test;

class ExtendsD extends D {
    void test() {
        int x = foo();
        Integer y = foo();
        Object z = foo();
    }
}

// FILE: ManyClassesHierarchy.kt
package test

interface A<T> {
    fun foo(): T
}

interface B : A<Int>

abstract class C : B

open class D : C() {
    override fun foo(): Int = 42
}
