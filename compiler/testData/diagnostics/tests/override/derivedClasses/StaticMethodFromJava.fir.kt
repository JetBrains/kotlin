// MODULE: lib
// FILE: test/J.java
package test;

class Foo<T> {
    public static <K> String getValue(K key) {
        return null;
    }
}

public class Bar extends Foo<String> {}

// MODULE: main(lib)
// FILE: k.kt
import test.Bar

fun test() {
    <!DEBUG_INFO_CALLABLE_OWNER("test.Foo.getValue in test.Foo")!>Bar.getValue("bar")<!>
}