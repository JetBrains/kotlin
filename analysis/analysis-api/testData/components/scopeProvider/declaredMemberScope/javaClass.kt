// class: JavaClass
// FILE: main.kt

// FILE: SuperInterface.java
public interface SuperInterface {
    public int getActualRandomNumber();

    public static int getRandomNumber() {
        return 3; // Decided by a fair dice roll.
    }
}

// FILE: SuperClass.java
public abstract class SuperClass implements SuperInterface {
    public static class NestedSuperClass { }

    public class InnerSuperClass { }

    @Override
    public int getActualRandomNumber() {
        return getRandomNumber();
    }

    public static int superFoo = 5;

    public static String superBar() {
        return "superBar";
    }
}

// FILE: JavaClass.java
public class JavaClass extends SuperClass {
    public static class NestedClass { }

    public class InnerClass { }

    public static int foo = 1;

    public static String bar() {
        return "bar";
    }

    public void hello() {
        System.out.println("hello");
    }
}
