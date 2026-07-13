// ENABLE_FOREIGN_ANNOTATIONS
// WITH_STDLIB
// FULL_JDK
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProhibitIllegalNotNullSmartCastsInEqualities
// LANGUAGE_FEATURE_TOGGLED: ProhibitNotNullSmartCastsBasedOnFlexibleComponentsInEqualities
// JSPECIFY_STATE: warn

// FILE: q/JavaType.java
package q;

import org.jspecify.annotations.Nullable;

public class JavaType {
    public static @Nullable JavaType DEFAULT = new JavaType();

    @Nullable
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
        j<!UNSAFE_CALL!>.<!>javaTypeWrapperMethod()
        j<!UNSAFE_CALL!>.<!>javaType.javaTypeMethod()
    }
}

fun t2(j: JavaTypeWrapper?) {
    if (j?.javaType !== JavaType.DEFAULT) return
    j<!UNSAFE_CALL!>.<!>javaTypeWrapperMethod()
}

fun t3() {
    val from42: JavaType? = JavaType.fromInt(42)
    if (JavaType.DEFAULT != from42) return
    from42<!UNSAFE_CALL!>.<!>javaTypeMethod()
}

fun t4() {
    val from42: JavaType? = JavaType.fromInt(42)
    if (JavaType.DEFAULT === from42) {
        <!DEBUG_INFO_EXPRESSION_TYPE("(q.JavaType..@Nullable() q.JavaType?)")!>from42<!>.javaTypeMethod()
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("(@Nullable() q.JavaType..@Nullable() q.JavaType?)"), RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>JavaType.DEFAULT<!>.javaTypeMethod()
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, flexibleType, funWithExtensionReceiver, functionDeclaration,
ifExpression, integerLiteral, javaFunction, javaProperty, javaType, localProperty, nullableType, primaryConstructor,
propertyDeclaration, safeCall, smartcast */
