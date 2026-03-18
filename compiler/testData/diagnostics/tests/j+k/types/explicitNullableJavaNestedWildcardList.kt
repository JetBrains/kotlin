// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// FIR_DUMP

// FILE: JavaWildcards.java
import java.util.List;

public class JavaWildcards {
    public static <T> List<? extends T> outList(List<? extends T> xs) { return xs; }
    public static <T> T firstOut(List<? extends T> xs) { return xs.get(0); }
}

// FILE: Test.kt
fun takeString(x: String) {}
fun takeNullableString(x: String?) {}

fun test(xs: List<String?>, ys: List<String>) {
    val a = JavaWildcards.outList<String?>(xs)
    val b = JavaWildcards.outList<String>(ys)

    val x1: String? = a[0]
    val y1: String = b[0]

    takeNullableString(JavaWildcards.firstOut<String?>(xs))
    takeString(JavaWildcards.firstOut<String?>(xs))

    takeString(JavaWildcards.firstOut<String>(ys))
    takeNullableString(JavaWildcards.firstOut<String>(ys))
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType, localProperty,
nullableType, outProjection, propertyDeclaration */