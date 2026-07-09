// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProhibitIllegalNotNullSmartCastsInEqualities
// LANGUAGE_FEATURE_TOGGLED: ProhibitNotNullSmartCastsBasedOnFlexibleComponentsInEqualities

// FILE: q/JavaType.java
package q;

public class JavaType {
    public static JavaType DEFAULT = new JavaType();
}

// FILE: q/main.kt
package q;

fun <T> id(t: T): T = t

fun f(a: Any) {
    val x = JavaType.DEFAULT
    if (a == x) {
        var b = id(x)
        b = <!NULL_FOR_NONNULL_TYPE!>null<!>
    }
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, flexibleType, functionDeclaration, ifExpression, javaProperty,
localProperty, nullableType, propertyDeclaration, typeParameter */
