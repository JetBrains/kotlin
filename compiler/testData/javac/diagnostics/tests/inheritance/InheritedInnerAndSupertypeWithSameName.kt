// FILE: a/x.java
package a;

public class x {

    public class y {}

}

// FILE: a/y.java
package a;

public class y extends x {

}

// FILE: a/c.java
package a;

public class c extends y {

    public y getY() { return null; }

}

// FILE: test.kt
package test

import a.c

fun test() = c().getY()