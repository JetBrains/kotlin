// TARGET_BACKEND: JVM

// MODULE: lib
// FILE: A.java
public class A {
    public String foo() { return null; }
}

// MODULE: main(lib)
// FILE: nullCheckOnInterfaceDelegation.kt
interface IFoo {
    fun foo(): String
}

class Derived : A(), IFoo {
    override fun foo() = super<A>.foo()
}

class Delegated : IFoo by Derived()

fun testReturnValue(): String =
    Delegated().foo()
