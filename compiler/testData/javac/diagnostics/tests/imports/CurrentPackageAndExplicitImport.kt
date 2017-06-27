// FILE: a/Y.java
package a;

public class Y {
    public void test() {}
}

// FILE: b/Y.java
package b;

public class Y {}

// FILE: b/T.java
package b;

import a.Y;

public class T {

    public Y getY() { return null; }

}

// FILE: b/b.kt
package b

fun test() = T().getY().test()
