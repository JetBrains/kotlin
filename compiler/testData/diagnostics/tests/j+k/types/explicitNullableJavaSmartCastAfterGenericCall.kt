// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// FIR_DUMP

// FILE: JavaUtil.java
public class JavaUtil {
    public static <T> T id(T x) { return x; }
}

// FILE: Test.kt
fun test(x: String?) {
    val y = JavaUtil.id<String?>(x)

    y<!UNSAFE_CALL!>.<!>length

    if (y != null) {
        val ok: Int = y.length
    }

    val z = JavaUtil.id(x)

    z<!UNSAFE_CALL!>.<!>length

    if (z != null) {
        val ok2: Int = z.length
    }
}

/* GENERATED_FIR_TAGS: flexibleType, ifExpression, javaFunction, javaType, localProperty,
nullableType, propertyDeclaration */