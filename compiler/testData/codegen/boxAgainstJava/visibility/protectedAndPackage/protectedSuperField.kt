// FILE: test/Foo.java
package test;

public class Foo {
    protected final String value;

    protected Foo(String value) {
        this.value = value;
    }
}

// FILE: test.kt
import test.Foo

class Bar : Foo("OK") {
    fun baz() = super.value
}

fun box(): String = Bar().baz()
