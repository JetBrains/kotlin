// MODULE: m1
// FILE: org/test/Foo.java

package org.test;

class Foo {
    static Foo create() { return Foo(); }
    void takeFoo(Foo f) {}
}

// MODULE: m2(m1)
// FILE: test.kt

package org.test

fun test() {
    Foo()
    val a: Foo = Foo.create()
    Foo().takeFoo(a)
}