// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: p/Base.java
package p;

public class Base {
    static final int X = 1;
}

// FILE: r/Other.java
package r;

public class Other {
    public static final int X = 2;
}

// MODULE: main(lib)
// FILE: q/D.java
package q;

import static r.Other.X;

public class D extends p.Base {
    public static final int Y = X;
}

// FILE: test.kt
const val K = q.D.Y

fun box(): String {
    val runtime = q.D::class.java.getField("Y").getInt(null)
    return if (K == runtime && K == 2) "OK" else "FAIL: K=$K runtime=$runtime"
}