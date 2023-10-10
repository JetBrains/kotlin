// ISSUE: KT-62467

// FILE: JavaClass.java

public class JavaClass {
    public static String X = null;
    public static String f() { return null; }
}

// FILE: main.kt

fun testWithStatic1(x1: String?): String {
    val x: String?
    x = x1 ?: JavaClass.X
    return x
}

fun testWithStatic2(x1: String?): String {
    var x: String? = null
    x = x1 ?: JavaClass.f()
    return x
}

fun testWithStatic3(x1: String?): String {
    val x: String? = x1 ?: JavaClass.X
    return <!RETURN_TYPE_MISMATCH!>x<!>
}

fun testWithStatic4(x1: String?, w: Int): String {
    var x: String?
    x = x1 ?: when (w) {
        42 -> JavaClass.X
        else -> JavaClass.f()
    }
    return x
}