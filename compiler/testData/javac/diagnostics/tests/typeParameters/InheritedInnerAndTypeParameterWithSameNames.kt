// FILE: a/x.java
package a;

public class x {
    public class T {}
}

// FILE: a/b.java
package a;

public class b<T> extends x {
    public T getT() { return null; }
}

// FILE: test.kt
package test

import a.b

fun test() = b<String>().getT()