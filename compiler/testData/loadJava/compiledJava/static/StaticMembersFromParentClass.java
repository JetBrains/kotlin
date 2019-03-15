// SKIP_IN_FIR_TEST
package test;

class Parent {
    public static int a = 1;
    public static int b = 2;
    public static void foo() {}
    public static void baz() {}
}

class Child extends Parent {
    public static String b = "3";
    public static int c = 4;
    public static void foo(int i) {}
    public static void bar() {}
    public static void bar(int i) {}
    public static void baz() {}
    public static void baz(int i) {}
}
