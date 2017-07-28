// FILE: a/x.java
package a;

public class x {}

// FILE: b/x.java
package b;

public class x {}

// FILE: c/x.java
package c;

public class x {}

// FILE: c/y.java
package c;

import a.x;
import b.*;

public class y {

    public x getX() { return null; }

}

// FILE: c/test.kt
package c

fun test() = y().getX()