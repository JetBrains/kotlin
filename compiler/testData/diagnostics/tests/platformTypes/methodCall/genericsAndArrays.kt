// FILE: p/J.java

package p;

public class J<T> {
    public void foo(Ref<T[]> r) {}
}

// FILE: p/Ref.java

package p;

public class Ref<T> {
    public static <T> Ref<T> create() { return null; }
}

// FILE: k.kt

import p.*

fun main(j: J<String>) {
    val r = Ref.create<Array<String>>()
    j.foo(r)
}
