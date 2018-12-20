// TARGET_BACKEND: JVM

// FILE: A.java

public class A {
    public int foo(int x, String ... args) {
        return x + args.length;
    }

    public static String[] ar = new String[] { "a", "b"};
}

// FILE: test.kt

fun bar(args: Array<String>?): Int {
    var res  = 0

    if (args != null) {
        res += A().foo(1, *args)
    }

    res += A().foo(1, *A.ar)

    return res
}

fun box(): String {
    if (bar(null) != 3) return "Fail"

    if (bar(A.ar) != 6) return "Fail"

    return "OK"
}
