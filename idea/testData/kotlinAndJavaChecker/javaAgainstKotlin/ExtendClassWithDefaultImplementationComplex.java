package test;

public class ExtendClassWithDefaultImplementationComplext {
    <error descr="Class 'Test1' must either be declared abstract or implement abstract method 'a()' in 'A'">public static class Test1 implements A</error> {

    }

    public static class Test2 extends AI implements A {

    }

    <error descr="Class 'Test3' must either be declared abstract or implement abstract method 'b()' in 'B'">public static class Test3 extends AI implements B</error> {

    }

    public static class Test4 extends BI implements B {

    }

    <error descr="Class 'Test5' must either be declared abstract or implement abstract method 'c()' in 'C'">public static class Test5 extends BI implements C</error> {

    }

    <error descr="Class 'Test6' must either be declared abstract or implement abstract method 'd()' in 'D'">public static class Test6 extends BI implements D</error> {

    }

    public static class Test7 extends BI implements S {

    }

    public static interface Test8 extends A {

    }

    public static abstract class Test9 implements A {

    }
}
