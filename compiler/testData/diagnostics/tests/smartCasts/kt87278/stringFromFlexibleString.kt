// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE_FEATURE_TOGGLED: ProhibitNotNullSmartCastsBasedOnFlexibleComponentsInEqualities
// LANGUAGE: +ProhibitIllegalNotNullSmartCastsInEqualities

// FILE: q/JavaUtils.java
package q;

public class JavaUtils {
    static String getString() { return ""; }
}

// FILE: q/main.kt
package q

fun <T> id(t: T): T = t

fun String?.onNullableString() = Unit
fun String.onString() = Unit

fun test(a: Any?) {
    if (JavaUtils.getString() == a) {
        <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.String..kotlin.String?)")!>a<!>.onNullableString()
        <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.String..kotlin.String?)")!>a<!>.onString()
        var x = id(a)
        x = null
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression, nullableType */
