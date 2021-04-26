// FILE: A.java

class A {
    private static int a = 1;
    private static void foo() {}

    protected static int b = 1;
    protected static void bar() {}
}

// FILE: B.java

class B extends A {
    public static int a = 1;
    public static void foo() {}
    public static void foo(int i) {}

    public static int b = 1;
    public static void bar() {}
    public static void bar(int i) {}
}

// FILE: test.kt

fun test() {
    A.<!INVISIBLE_REFERENCE!>a<!>
    A.<!INVISIBLE_REFERENCE!>foo<!>()
    A.b
    A.bar()
    B.a
    B.foo()
    B.foo(1)
    B.b
    B.bar()
    B.bar(1)
}
