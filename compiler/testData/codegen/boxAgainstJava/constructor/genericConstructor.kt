// FILE: test/Foo.java

package test;

public class Foo<T extends Number> {
    public Foo(T number) {}
}

// FILE: 1.kt

import test.Foo

class Subclass : Foo<Int>(42) {
}

fun box(): String {
    Subclass()
    return "OK"
}
