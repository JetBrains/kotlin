// FILE: a/X.java
package a;

public class X {}

// FILE: b/X.java
package b;

public class X {}

// FILE: c/Test.java
package c;

import a.X;
import b.*;

public class Test {

    public X test() { return null; };

}

// FILE: c.kt
package c

fun test() = Test().test()
