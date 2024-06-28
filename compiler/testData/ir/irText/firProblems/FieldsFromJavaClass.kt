// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// ISSUE: KT-61362, KT-65722
// DUMP_EXTERNAL_CLASS: J
// DUMP_EXTERNAL_CLASS: X
// DUMP_EXTERNAL_CLASS: J1
// DUMP_EXTERNAL_CLASS: X1
// DUMP_IR

// FILE: J.java

public class J {
    public int f = 0;
    public static int s = 0;

    public int f2 = 0;
    public static int s2 = 0;
}

// FILE: X.java

public class X extends J {
    public int f2 = 1;
    public static int s2 = 1;
}

// FILE: J1.java

public class J1<T> {
    public T f = null;
    public static T s = null;

    public T f2 = null;
    public static T s2 = null;
}

// FILE: X1.java

class X1<T> extends J1<String> {
    public String f2 = "s1";
    public static String s2 = "s2";
}

// FILE: Main.kt

fun f(j: J, x: X, j1: J1<String>) {
    val jf = j::f
    val js = J::s
    val xf = x::f
    val xs = X::s
    val xf2 = x::f2
    val xs2 = X::s2
    val j1f = j1::f
}
