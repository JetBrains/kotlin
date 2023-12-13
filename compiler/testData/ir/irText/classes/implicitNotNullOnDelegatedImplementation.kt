// TARGET_BACKEND: JVM

// FILE: implicitNotNullOnDelegatedImplementation.kt
interface IFoo {
    fun foo(): String
}

class K1 : JFoo()

class K2 : JFoo() {
    override fun foo() = super.foo()
}

class K3 : JUnrelatedFoo(), IFoo

class K4 : JUnrelatedFoo(), IFoo {
    override fun foo() = super.foo()
}

class TestJFoo : IFoo by JFoo() {
    // nullability assertion in 'foo()'
}

class TestK1 : IFoo by K1() {
    // nullability assertion in 'foo()'
}

class TestK2 : IFoo by K2() {
    // no nullability assertion in 'foo()'
}

class TestK3 : IFoo by K3() {
    // no nullability assertion in 'foo()'
}

class TestK4 : IFoo by K4() {
    // nullability assertion in 'foo()'
}


// FILE: JFoo.java
public class JFoo implements IFoo {
    public String foo() { return null; }
}

// FILE: JUnrelatedFoo.java
public class JUnrelatedFoo {
    public String foo() { return null; }
} 
