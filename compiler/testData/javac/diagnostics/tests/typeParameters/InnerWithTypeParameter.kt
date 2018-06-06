// FILE: a/x.java
package a;

public class x<T> {

    public T getT() { return null; }

    public class y<Z> {
        public T getT() { return null; }
        public Z getZ() { return null; }
    }

}

// FILE: test.kt
package test

import a.*

fun test() = x<String>().getT()
fun test2() = x<Int>().y<String>().getT()
fun test3() = x<Int>().y<String>().getZ()