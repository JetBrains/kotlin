// TARGET_BACKEND: JVM
// FILE: p/Main.kt
package p

const val FOO = 42

// FILE: q/JavaUsesFacade.java
package q;

import p.MainKt;

public interface JavaUsesFacade {
    int X = MainKt.FOO;
}

// FILE: test.kt
package q

const val K = JavaUsesFacade.X

fun box(): String = if (K == 42) "OK" else "FAIL: $K"