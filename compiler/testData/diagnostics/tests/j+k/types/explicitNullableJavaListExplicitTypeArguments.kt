// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// FIR_DUMP

// FILE: JavaLists.java
import java.util.List;

public class JavaLists {
    public static <T> List<T> id(List<T> xs) { return xs; }
    public static <T> T first(List<T> xs) { return xs.get(0); }
}

// FILE: Test.kt
fun takeString(x: String) {}
fun takeNullableString(x: String?) {}

fun test(xs: List<String?>, ys: List<String>) {
    takeNullableString(JavaLists.first<String?>(xs))
    takeString(<!ARGUMENT_TYPE_MISMATCH!>JavaLists.first<String?>(xs)<!>)

    takeString(JavaLists.first<String>(ys))
    takeNullableString(JavaLists.first<String>(ys))

    val a: List<String?> = JavaLists.id<String?>(xs)
    val b: List<String> = JavaLists.id<String>(ys)

    val c: String? = JavaLists.id<String?>(xs)[0]
    val d: String = JavaLists.id<String>(ys)[0]

    val badElement: String <!INITIALIZER_TYPE_MISMATCH!>=<!> JavaLists.id<String?>(xs)[0]
    val badList: List<String> <!INITIALIZER_TYPE_MISMATCH!>=<!> JavaLists.id<String?>(xs)
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType, localProperty,
nullableType, propertyDeclaration */
