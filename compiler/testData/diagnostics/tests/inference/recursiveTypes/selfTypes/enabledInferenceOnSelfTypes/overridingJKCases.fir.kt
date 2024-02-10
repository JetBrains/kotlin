// ISSUE: KT-59012
// !LANGUAGE: +TypeInferenceOnCallsWithSelfTypes
// FILE: I.java
public interface I<G extends I<G>> {
    <T extends G> T foo();
}

// FILE: J.java
public class J<G extends J<G>> {
    <T extends G> void foo(T...args) {}
}

// FILE: Main.kt
class A1<G: A1<G>> : I<G> {
    override fun <T : G> foo(): T = TODO()
}

class B1<G: I<G>> : I<G> {
    override fun <T : G> foo(): T = TODO()
}

abstract class C1<G: I<G>> : I<G>

class A2<G: A2<G>> : J<G>() {}

class B2<G: J<G>> : J<G>() {}

class C2<G: C2<G>> : J<G>() {
    public override fun <T : G> foo(vararg args: T) {}
}

class D2<G: J<G>> : J<G>() {
    public override fun <T : G> foo(vararg args: T) {}
}

fun javaInterfaceTest(a: A1<*>, b: B1<*>, c: C1<*>) {
    val x = a.foo()
    <!DEBUG_INFO_EXPRESSION_TYPE("A1<*>")!>x<!>
    val y = b.foo()
    <!DEBUG_INFO_EXPRESSION_TYPE("I<*>")!>y<!>
    val z = c.foo()
    <!DEBUG_INFO_EXPRESSION_TYPE("I<*>..I<*>?!")!>z<!>
}

fun javaClassTest(a: A2<*>, b: B2<*>, c: C2<*>, d: D2<*>) {
    a.foo()
    b.foo()
    c.foo()
    d.foo()
}
