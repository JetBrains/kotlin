// TARGET_BACKEND: JVM

// FILE: Child.java

class Child extends Parent {
    public static String a = "2";
    public static String foo() {
        return "Child.foo()";
    }
    public static String foo(int i) {
        return "Child.foo(int)";
    }
}

// FILE: Parent.java

class Parent {
    private static int a = 1;
    private static String foo() {
        return "Parent.foo";
    }
}

// FILE: test.kt

fun box(): String {
    if (Child.a != "2") return "Fail #1"
    if (Child.foo() != "Child.foo()") return "Fail #2"
    if (Child.foo(1) != "Child.foo(int)") return "Fail #3"

    return "OK"
}
