// FIR_IDENTICAL
// MODULE: m1
// FILE: test/Foo.java

package test;

class Foo {
    static Foo create() { return Foo(); }
    void takeFoo(Foo f) {}
}

// MODULE: m2(m1)
// FILE: test.kt

package test

fun test() {
    Foo()
    val a: Foo = Foo.create()
    Foo().takeFoo(a)
}
