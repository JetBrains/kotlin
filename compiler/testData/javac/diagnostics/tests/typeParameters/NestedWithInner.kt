// FILE: a/x.java
package a;

public class x<T> {

    public static class Nested {

        public T getT() { return null; }

        public class T {

            public T getT() { return null; }

        }

    }

}

// FILE: test.kt
package test

import a.*

fun test() = x.Nested().getT().getT()