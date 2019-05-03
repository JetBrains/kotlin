interface A {
    fun foo(): Any
}

open class B {
    fun foo(): String = "hello"
}

open class C : B()

class D : A, C() {
    // There's a fake override for 'foo' in class D, and, since there are declarations with different signatures in the supertypes,
    // a bridge is generated with the signature foo()Ljava/lang/Object; which invokes foo()Ljava/lang/String;
    // This test checks that the generated bridge calls D.foo() instead of B.foo(). That way when an implementation of
    // foo()Ljava/lang/String is added to C later, D won't need to be recompiled.
    // Note that invokevirtual/invokespecial of C.foo() would also be fine (invokespecial is javac's behavior).
}

// 1 INVOKEVIRTUAL D.foo
// 0 INVOKEVIRTUAL B.foo
