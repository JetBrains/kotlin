// ISSUE: KT-57811, KT-61786

// FILE: JavaClass.java

public class JavaClass {
    public static final String X = null;
}

// FILE: main.kt

fun test(x1: String?): String {
    val x: String?
    x = x1 ?: JavaClass.X
    return x
}