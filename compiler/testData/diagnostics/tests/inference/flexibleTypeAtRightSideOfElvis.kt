// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-62467

// FILE: JavaClass.java
import java.util.List;

public class JavaClass {
    public static String X = null;
    public static String f() { return null; }

    @org.jetbrains.annotations.NotNull
    public static List<String> LIST = null;
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

fun testWithFlexibleMutable(x1: List<String>?): List<String> {
    val x: List<String>?
    x = x1 ?: JavaClass.LIST
    return x
}

/* GENERATED_FIR_TAGS: assignment, elvisExpression, equalityExpression, flexibleType, functionDeclaration,
integerLiteral, javaFunction, javaProperty, localProperty, nullableType, propertyDeclaration, smartcast, whenExpression,
whenWithSubject */
