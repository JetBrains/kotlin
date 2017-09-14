// FILE: a/x.java
package a;

public class x {

    public class z {}

}

// FILE: a/y.java
package a;

public class y extends x {

    public z getZ() { return null; }

    public class d {

        public z getZ() { return null; }

    }

}

// FILE: test.kt
package test

import a.y

fun test() = y().getZ()
fun test2() = y().d().getZ()