// FILE: a/x.java
package a;

public class x {

    public b getB() { return null; }

    public class b {
        public b getB() { return null; }
    }

}

// FILE: a/b.java
package a;

public class b {}

// FILE: test/test.kt
package test

import a.x

fun test() = x().getB()
fun test2() = test().getB()
