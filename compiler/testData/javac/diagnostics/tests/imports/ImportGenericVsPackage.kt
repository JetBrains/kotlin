// FILE: a/x.java
package a;

public class x<T> {}

// FILE: b/x.java
package b;

public class x {}

// FILE: b/test.java
package b;

import a.x;

public class test {

    public x<?> getX() { return null; }

}

// FILE: b/test.kt
package b

fun test1() = test().getX()