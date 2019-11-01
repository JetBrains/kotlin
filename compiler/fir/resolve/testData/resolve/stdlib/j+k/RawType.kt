// FILE: JavaClass.java

public class JavaClass {
    public static void foo(ArrayList list) {}
}

// FILE: test.kt

class Some

fun test(list: ArrayList<Some>) {
    JavaClass.foo(list)
}