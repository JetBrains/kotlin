// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79864

import kotlin.reflect.KClass

val <T> KClass<T & Any>.myJava: Class<T> get() = null!!

fun <F : Enum<*>> field(defValue: F) {
    val tmp = defValue::class.myJava
    java.lang.Enum.valueOf(tmp, "str")

    java.lang.Enum.valueOf(<!ARGUMENT_TYPE_MISMATCH!>defValue::class.myJava<!>, "str")

    expect {
        val tmp = defValue::class.myJava
        java.lang.Enum.valueOf(tmp, "str")
    }

    expect {
        java.lang.Enum.valueOf(<!ARGUMENT_TYPE_MISMATCH!>defValue::class.myJava<!>, "str")
    }
}

fun expect(f: (String) -> Unit) {}

/* GENERATED_FIR_TAGS: capturedType, checkNotNullCall, classReference, flexibleType, functionDeclaration, functionalType,
getter, javaFunction, lambdaLiteral, localProperty, nullableType, propertyDeclaration, propertyWithExtensionReceiver,
starProjection, stringLiteral, typeConstraint, typeParameter */
