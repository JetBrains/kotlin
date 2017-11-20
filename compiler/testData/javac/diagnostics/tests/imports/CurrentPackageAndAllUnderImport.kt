// FILE: a/X.java
package a;

public class X {}

// FILE: b/X.java
package b;

public class X {}

// FILE: b/A.java
package b;

import a.*;

public class A {

    public X getX() { return null; }

}

// FILE: b.kt
package b

fun test() = A().getX()
