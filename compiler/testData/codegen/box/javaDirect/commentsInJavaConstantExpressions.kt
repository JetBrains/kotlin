// TARGET_BACKEND: JVM
// FILE: p/C.java
package p;

public class C {
    public static final int X = 1 /* one */ + 2;
    public static final int T = /* default */ 10;
    public static final int P = 1 + /* a */ 2 + 3;
    public static final int N = - /* negative */ 1;
    public static final byte B = (byte) /* cast */ 300;
}

// FILE: test.kt
const val KX = p.C.X
const val KT = p.C.T
const val KP = p.C.P
const val KN = p.C.N
const val KB = p.C.B

fun box(): String = if (KX == 3 && KT == 10 && KP == 6 && KN == -1 && KB.toInt() == 44) "OK" else "FAIL: $KX $KT $KP $KN $KB"