// FULL_JDK
// FILE: JavaClass.java
import java.util.ArrayList;

public class JavaClass {
    public static void foo(ArrayList list) {}
}

// FILE: test.kt

class Some

fun test(list: ArrayList<Some>) {
    JavaClass.foo(list)
}
