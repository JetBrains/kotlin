// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: ProhibitNotNullSmartCastsBasedOnFlexibleComponentsInEqualities
// LANGUAGE: +ProhibitIllegalNotNullSmartCastsInEqualities

// FILE: q/JavaType.java
package q;

public enum JavaType {
    A, B;

    public static JavaType DEFAULT = A;

    public static JavaType fromInt(int i){
        return B;
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
        <!DEBUG_INFO_EXPRESSION_TYPE("q.JavaType")!>from42<!>.javaTypeMethod()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression,
integerLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration, safeCall */
