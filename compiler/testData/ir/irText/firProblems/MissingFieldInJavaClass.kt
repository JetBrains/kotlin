// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// ISSUE: KT-61362
// DUMP_EXTERNAL_CLASS: J
// DUMP_EXTERNAL_CLASS: X
// DUMP_EXTERNAL_CLASS: J1
// DUMP_EXTERNAL_CLASS: X1

// FILE: J.java

class J {
    public int f = 0;
    public static int s = 0;

    public int f2 = 0;
    public static int s2 = 0;
}

// FILE: X.java

class X extends J {
    public int f2 = 1;
    public static int s2 = 1;
}

// FILE: J1.java

class J1<T> {
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