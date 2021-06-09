// FIR_IDENTICAL
//FILE:a/Foo.java
package a;

public class Foo {
    public void bar() {

    }

    public static void bar(String arg) {

    }
}

//FILE:test.kt
package b

import a.Foo.bar
