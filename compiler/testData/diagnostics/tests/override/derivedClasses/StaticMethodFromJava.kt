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
    Bar.<!DEBUG_INFO_CALLABLE_OWNER("test.Bar.getValue in test.Bar")!>getValue("bar")<!>
}