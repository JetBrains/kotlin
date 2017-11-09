// FILE: a/x.kt
package a

open class x {

    inner class y

}

// FILE: a/b.java
package a;

public class b extends x {

    public y getY() { return null; }

}

// FILE: test.kt
package test

import a.b

fun test() = b().getY()