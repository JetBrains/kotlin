// FILE: C.kt

class C : B() {
    override fun foo() {}
}

// FILE: B.java

public class B extends A {
    @java.lang.Override
    public void foo() {}
}

// FILE: A.kt

abstract class A {
    abstract fun foo()
}
