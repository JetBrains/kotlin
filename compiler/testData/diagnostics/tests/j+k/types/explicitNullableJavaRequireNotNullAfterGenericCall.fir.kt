// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// FIR_DUMP
// WITH_STDLIB

// FILE: JavaUtil.java
public class JavaUtil {
    public static <T> T id(T x) { return x; }
}

// FILE: Test.kt
fun test(x: String?) {
    val y = JavaUtil.id<String?>(x)
    y<!UNSAFE_CALL!>.<!>length

    requireNotNull(y)
    val ok: Int = y.length

    val z = JavaUtil.id(x)
    z<!UNSAFE_CALL!>.<!>length

    requireNotNull(z)
    val ok2: Int = z.length
}

/* GENERATED_FIR_TAGS: flexibleType, javaFunction, javaType, localProperty, nullableType,
propertyDeclaration */