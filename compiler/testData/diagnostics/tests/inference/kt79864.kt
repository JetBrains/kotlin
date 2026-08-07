// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-79864

import kotlin.reflect.KClass

val <T> KClass<T & Any>.myJava: Class<T> get() = null!!

fun <F : Enum<*>> field(defValue: F) {
    val tmp = defValue::class.myJava
    java.lang.Enum.valueOf(tmp, "str")

    java.lang.Enum.valueOf(defValue::class.myJava, "str")

    expect {
        val tmp = defValue::class.myJava
        java.lang.Enum.valueOf(tmp, "str")
    }

    expect {
        java.lang.Enum.valueOf(defValue::class.myJava, "str")
    }
}

fun expect(f: (String) -> Unit) {}

/* GENERATED_FIR_TAGS: capturedType, checkNotNullCall, classReference, flexibleType, functionDeclaration, functionalType,
getter, javaFunction, lambdaLiteral, localProperty, nullableType, propertyDeclaration, propertyWithExtensionReceiver,
starProjection, stringLiteral, typeConstraint, typeParameter */
