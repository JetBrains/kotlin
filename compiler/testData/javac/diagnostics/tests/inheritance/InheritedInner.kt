// FILE: a/x.java
package a;

public class x {

    public class y {}

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