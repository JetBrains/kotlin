// FILE: Child.java

class Child extends Parent {
    public static int b = 3;
    public static int c = 4;
    public static void bar() {}
    public static void baz() {}
}

// FILE: Parent.java

class Parent {
    public static int a = 1;
    public static int b = 2;
    public static void foo() {}
    public static void baz() {}
}

// FILE: test.kt

fun test() {
    Parent.a
    Parent.b
    Parent.foo()
    Parent.baz()

    Child.a
    Child.b
    Child.c
    Child.foo()
    Child.bar()
    Child.baz()
}

// 1 GETSTATIC Parent.a : I
// 1 GETSTATIC Parent.b : I
// 1 INVOKESTATIC Parent.foo()
// 1 INVOKESTATIC Parent.baz()
// 1 GETSTATIC Child.a : I
// 1 GETSTATIC Child.b : I
// 1 GETSTATIC Child.c : I
// 1 INVOKESTATIC Child.foo()
// 1 INVOKESTATIC Child.bar()
// 1 INVOKESTATIC Child.baz()
