// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: ProhibitNotNullSmartCastsBasedOnFlexibleComponentsInEqualities
// LANGUAGE: +ProhibitIllegalNotNullSmartCastsInEqualities

// FILE: q/JavaType.java
package q;

public class JavaType {
    public static JavaType DEFAULT = new JavaType();

    public static JavaType fromInt(int i){
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
        from42.javaTypeMethod()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression,
integerLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration, safeCall */
