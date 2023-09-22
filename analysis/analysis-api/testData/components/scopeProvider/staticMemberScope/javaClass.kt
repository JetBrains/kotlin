// class: JavaClass
// FILE: main.kt

// FILE: SuperInterface.java
public interface SuperInterface {
    public int getActualRandomNumber();

    // This static method will not be included in JavaClass's static member scope, because static interface methods are not propagated to
    // their implementing class's namespaces (i.e., `JavaClass.getRandomNumber()` is an error in Kotlin and Java).
    public static int getRandomNumber() {
        return 3; // Decided by a fair dice roll.
    }
}

// FILE: SuperClass.java
public abstract class SuperClass implements SuperInterface {
    // NestedSuperClass will not be included in JavaClass's static member scope, because Kotlin does not propagate static nested classes
    // defined in super-classes to sub-classes, unlike Java. So `JavaClass.SuperClass` is valid in Java, but an error in Kotlin.
    public static class NestedSuperClass { }

    public class InnerSuperClass { }

    @Override
    public int getActualRandomNumber() {
        return getRandomNumber();
    }

    // Both static callables will be included in JavaClass's static member scope, because Kotlin propagates static methods from
    // super-classes to sub-classes, just like Java. So `JavaClass.superBar()` will be a valid call in Kotlin and Java.
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
