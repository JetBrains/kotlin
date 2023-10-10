// ISSUE: KT-62467

// FILE: JavaClass.java

public class JavaClass {
    public static String X = null;
}

// FILE: main.kt

fun testWithStatic(): String {
    val x: String?
    x = null ?: JavaClass.X
    return x
}