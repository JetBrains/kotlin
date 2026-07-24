// ENABLE_FOREIGN_ANNOTATIONS
// WITH_STDLIB
// FULL_JDK
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ProhibitIllegalNotNullSmartCastsInEqualities
// LANGUAGE_FEATURE_TOGGLED: ProhibitNotNullSmartCastsBasedOnFlexibleComponentsInEqualities
// LANGUAGE_FEATURE_TOGGLED_IDENTICAL
// JSPECIFY_STATE: strict

// FILE: q/JavaType.java
package q;

import org.jetbrains.annotations.NotNull;

public class JavaType {
    @NotNull
    public static JavaType DEFAULT = new JavaType();

    @NotNull
    public static JavaType fromInt(int i) {
        return new JavaType();
    }
}

// FILE: q/main.kt
package q

fun JavaType.javaTypeMethod() = Unit

class JavaTypeWrapper(val javaType: JavaType) {
    fun javaTypeWrapperMethod() = Unit
}

fun t1(j: JavaTypeWrapper?) {
    if (j?.javaType == JavaType.DEFAULT) {
        j.javaTypeWrapperMethod()
        j.javaType.javaTypeMethod()
    }
}

fun t2(j: JavaTypeWrapper?) {
    if (j?.javaType !== JavaType.DEFAULT) return
    j.javaTypeWrapperMethod()
}

fun t3() {
    val from42: JavaType? = JavaType.fromInt(42)
    if (JavaType.DEFAULT != from42) return
    from42.javaTypeMethod()
}

fun t4() {
    val from42: JavaType? = JavaType.fromInt(42)
    if (JavaType.DEFAULT === from42) {
        from42.javaTypeMethod()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression,
integerLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration, safeCall */
