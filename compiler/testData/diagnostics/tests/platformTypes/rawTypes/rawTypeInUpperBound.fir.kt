// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.List;

public class A<T> {
    List<String> getChildrenStubs() { return null; }
    void consume(T x) {}

    T produce() { return null; }
}

// FILE: B.java

public class B<E extends A> {
    public E foo() { return null;}
    E field;
}

// FILE: Test.java

public class Test {
    static B rawB = null;
}

// FILE: main.kt

fun foo(x: B<*>) {
    // TODO: In K1, x.foo() now is flexible type instead of raw, because of captured type approximation
    // Works in K2 as expected: x.foo() returns raw `A`, thus it's `getChildrenStubs` has a type `MutableList<Any!>..List<*>?`
    val q: MutableList<String> = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>x.foo().getChildrenStubs()<!>

    // Raw(B).field erased to A<Any!>..A<out Any!>?
    Test.rawB.field = A<String>()
    val anyA: A<Any> = Test.rawB.field

    // FIR doesn't work here, because
    // field has a type of just 'A' and it's not clear why should it accept 'String' at consume
    // NB: some kind of BareTypeScope should be in use here
    Test.rawB.field.consume("")
    val y: Any = Test.rawB.field.produce()
}
