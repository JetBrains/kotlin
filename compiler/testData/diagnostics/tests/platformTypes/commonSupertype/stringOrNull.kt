// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: p/Utils.java

package p;

public class Utils {
    public static String str() { return null; }
}

// FILE: k.kt

import p.*

fun <T : Any> T.foo() {}

fun <D> test(b: Boolean) {
    val str = if (b) Utils.str() else null

    str?.foo()
}

/* GENERATED_FIR_TAGS: flexibleType, funWithExtensionReceiver, functionDeclaration, ifExpression, javaFunction,
localProperty, nullableType, propertyDeclaration, safeCall, typeConstraint, typeParameter */
