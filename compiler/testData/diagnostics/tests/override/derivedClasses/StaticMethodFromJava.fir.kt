// MODULE: lib

// FILE: test/Foo.java
package test;

class Foo<T> {
    public static <K> String getValue(K key) {
        return null;
    }
}

// FILE: test/Bar.java
package test;

public class Bar extends Foo<String> {}

// MODULE: main(lib)
// FILE: k.kt
import test.Bar

fun test() {
    <!DEBUG_INFO_CALLABLE_OWNER("test.Foo.getValue in test.Foo")!>Bar.getValue("bar")<!>
}