// TARGET_BACKEND: JVM
// DISABLE_PARAM_ASSERTIONS

// MODULE: lib
// FILE: A.java

import org.jetbrains.annotations.NotNull;

public class A {
    @NotNull
    public final String NULL = null;

    @NotNull
    public static final String STATIC_NULL = null;

    public String foo() {
        return null;
    }

    public static String staticFoo() {
        return null;
    }

    public A plus(A a) {
        return null;
    }

    public A inc() {
        return null;
    }

    public Object get(Object o) {
        return null;
    }

    public A a() { return this; }

    public static class B {
        public static B b() { return null; }
    }

    public static class C {
        public static C c() { return null; }
    }
}

// MODULE: main(lib)
// FILE: callAssertions.kt

class AssertionChecker(val nullPointerExceptionExpected: Boolean) {
    operator fun invoke(name: String, f: () -> Any) {
        try {
            f()
        } catch (e: NullPointerException) {
            if (!nullPointerExceptionExpected) throw AssertionError("Unexpected NullPointerException on calling $name")
            return
        }
        if (nullPointerExceptionExpected) throw AssertionError("NullPointerException expected on calling $name")
    }
}


interface Tr {
    fun foo(): String
}

class Derived : A(), Tr {
    override fun foo() = super<A>.foo()
}

class Delegated : Tr by Derived() {
}

fun checkAssertions(illegalStateExpected: Boolean) {
    val check = AssertionChecker(illegalStateExpected)

    // simple call
    check("A::foo") { A().foo() }

    // simple static call
    check("A::staticFoo") { A.staticFoo() }

    // super call
    check("Derived::foo") { Derived().foo() }

    // delegated call
    check("Delegated::foo") { Delegated().foo() }

    // collection element
    check("A::get") { A()[""] }

    // binary expression
    check("A::plus") { A() + A() }

    // field
    check("A::NULL") { A().NULL }

    // static field
    check("A::STATIC_NULL") { A.STATIC_NULL }

    // postfix expression
    // TODO:
//    check("A::inc") { var a = A().a(); a++ }

    // prefix expression
    check("A::inc-b") { var a = A.B.b(); a++ }

    // prefix expression
    check("A::inc-c") { var a = A.C.c(); a++ }

    // prefix expression
    check("A::inc") { var a = A().a(); ++a }

    // prefix expression
    check("A::inc-b") { var a = A.B.b(); ++a }

    // prefix expression
    // TODO:
//    check("A::inc-c") { var a = A.C.c(); ++a }
}

operator fun A.C.inc(): A.C = A.C()
operator fun <T> T.inc(): T = null as T

fun box(): String {
    checkAssertions(true)
    return "OK"
}
