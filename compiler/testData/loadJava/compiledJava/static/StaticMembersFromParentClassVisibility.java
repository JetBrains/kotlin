// SKIP_IN_FIR_TEST
package test;

class Parent {
    private static int private_ = 1;
    static int packagePrivate_ = 2;
    protected static int protected_ = 3;
    public static int public_ = 4;
}

class Child extends Parent {
}
