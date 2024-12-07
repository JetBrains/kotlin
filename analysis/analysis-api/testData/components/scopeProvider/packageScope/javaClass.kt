// FILE: SuperInterface.java
package test.api;

public interface SuperInterface {
    public int getActualRandomNumber();

    public static int getRandomNumber() {
        return 3; // Decided by a fair dice roll.
    }
}

// FILE: SuperClass.java
package test;

import test.api.SuperInterface;

public abstract class SuperClass implements SuperInterface {
    public static class NestedSuperClass { }

    public class InnerSuperClass { }

    @Override
    public int getActualRandomNumber() {
        return getRandomNumber();
    }

    public int superField = 1;

    public static int superFoo = 5;

    public static String superBar() {
        return "superBar";
    }
}

// FILE: JavaClass.java
package test;

public class JavaClass extends SuperClass {
    public static class NestedClass { }

    public class InnerClass { }

    public static int foo = 1;

    public static String bar() {
        return "bar";
    }

    public int field = 1;

    public void hello() {
        System.out.println("hello");
    }

    public <T> void method(Class<? extends T> ... classes) {

    }
}

// FILE: main.kt
package test

class KotlinClass : JavaClass()

// package: test
