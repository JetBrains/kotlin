// ISSUE: KT-62467, KT-57811, KT-61786

// FILE: JavaClass.java

public class JavaClass {
    public static String X = null;
    public final static String Y = null;
}

// FILE: main.kt

fun testWithStatic(): String {
    val x: String?
    x = null ?: JavaClass.X
    return x
}

fun testWithFinalStatic(): String {
    val y: String?
    y = null ?: JavaClass.Y
    return y
}