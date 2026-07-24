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

fun Any.callOnNotNull() = Unit

fun <T> f(a: T) {
    val x = JavaType.DEFAULT
    if (a == x) {
        a.callOnNotNull()
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, flexibleType, funWithExtensionReceiver, functionDeclaration, ifExpression,
javaProperty, localProperty, nullableType, propertyDeclaration, typeParameter */
