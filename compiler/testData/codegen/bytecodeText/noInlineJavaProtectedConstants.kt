// LANGUAGE_VERSION: 1.0
// FILE: first/Foo.java

package first;

public class Foo {
    protected static final int FOO = 42;
}

// FILE: bar.kt

package second

import first.Foo

class Bar : Foo() {
    fun bar() = FOO
}

// @second/BarKt.class
// 1 INVOKESTATIC
// 0 GETSTATIC
// 1 BIPUSH 42