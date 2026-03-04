// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79864
// DUMP_INFERENCE_LOGS: FIXATION, MARKDOWN

import kotlin.reflect.KClass

interface Owner {
    val <C> KClass<C & Any>.myJava: Class<C>

    fun <F : Enum<*>> field(defValue: F) {
        // valueOf: Expected argument: Class<T!>!, actual argument: Class<C>
        // myJava: Expected receiver: KClass<C & Any> (or KClass<C>), actual receiver: KClass<F>
        // C & Any <: F ==> C <: F?    F <: C & Any  ==> F <: C
        java.lang.Enum.valueOf(<!ARGUMENT_TYPE_MISMATCH!>defValue::class.myJava<!>, "str")
    }
}

/* GENERATED_FIR_TAGS: capturedType, checkNotNullCall, classReference, flexibleType, functionDeclaration, functionalType,
getter, javaFunction, lambdaLiteral, localProperty, nullableType, propertyDeclaration, propertyWithExtensionReceiver,
starProjection, stringLiteral, typeConstraint, typeParameter */
