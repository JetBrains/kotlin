// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java
public class A {
    static void foo() {}
    int foo;

    static void foo2() {}
    int foo2;

    static int bar;
    int bar() {return 1;}

    static int bar2;
    int bar2() {return 1;}

    static class quux {}
    void quux() {}
    int quux;

    static class quux2 {}
    void quux2() {}
    int quux2;

    static void baz() {}
}

// FILE: B.java
public class B extends A {
    void baz(int i) {}
}


// FILE: 1.kt
// Below should be all good because there is always a static function, property, or class with the requested name.
import A.foo
import A.bar
import A.quux
import B.foo2
import B.bar2
import A.baz
import B.baz

// class cannot be imported by subclass
import B.<!CANNOT_BE_IMPORTED!>quux2<!>
