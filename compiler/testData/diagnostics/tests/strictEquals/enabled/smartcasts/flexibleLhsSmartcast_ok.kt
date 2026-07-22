// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ProhibitNotNullSmartCastsBasedOnFlexibleComponentsInEqualities
// LANGUAGE: +ProhibitIllegalNotNullSmartCastsInEqualities

// FILE: q/Utils.java
package q;

public class Utils {
    public static <T> T makeFlexible(T t) {
        return t;
    }
}

// FILE: q/main.kt
package q

open class Regular {
    override fun equals(@EqualityBound(Regular::class) other: Any?): Boolean = true
}

fun Regular?.methodOnNullable() = Unit
fun Regular.methodOnNotNull() = Unit

fun <T> id(t: T): T = t

fun useSite_1(b: Any?) {
    val a = Utils.makeFlexible(Regular())
    if (a == b) {
        b.methodOnNullable()
        b?.methodOnNotNull()

        var c = id(b)
        c = null
    }
}

fun <T : Regular> useSite_2(a: T, b: Any?) {
    val a1 = Utils.makeFlexible(a)
    if (a1 == b) {
        b.methodOnNullable()
        b?.methodOnNotNull()

        var c = id(b)
        c = null
    }
}

fun <T : Regular?> useSite_3(a: T, b: Any?) {
    val a1 = Utils.makeFlexible(a)
    if (a1 == b) {
        b.methodOnNullable()
        b?.methodOnNotNull()

        var c = id(b)
        c = null
    }
}

fun useSite_4(b: Any) {
    val a = Utils.makeFlexible(Regular())
    if (a == b) {
        b.methodOnNullable()
        b.methodOnNotNull()
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, classReference, equalityExpression, flexibleType,
funWithExtensionReceiver, functionDeclaration, ifExpression, javaFunction, localProperty, nullableType, operator,
override, propertyDeclaration, safeCall, smartcast, typeConstraint, typeParameter */
