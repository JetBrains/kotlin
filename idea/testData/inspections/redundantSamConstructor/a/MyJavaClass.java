package a;

public class MyJavaClass {
    public static void staticFun1(Runnable r) {}
    public static void staticFun2(Runnable r1, Runnable r2) {}
    public static void staticFunWithOtherParam(int r1, Runnable r2) {}

    public void memberFun1(Runnable r) {}
    public void memberFun2(Runnable r1, Runnable r2) {}
    public void memberFunWithOtherParam(int r1, Runnable r2) {}

    public static <T> void foo1(Runnable r, T t) {}
    public static <T> void foo2(T t, Runnable r) {}
}