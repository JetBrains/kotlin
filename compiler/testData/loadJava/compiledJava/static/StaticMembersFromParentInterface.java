// SKIP_IN_FIR_TEST
package test;

interface Parent1 {
    public static int a = 1;
    public static int b = 2;
}

interface Parent2 {
    public static int d = 1;
    public static int e = 2;
}

class Child implements Parent1, Parent2 {
    public static String b = "3";
    public static int c = 4;
    public static String d = "4";
    public static void bar() {}
    public static void baz() {}
}
