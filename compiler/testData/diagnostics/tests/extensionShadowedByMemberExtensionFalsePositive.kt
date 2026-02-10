// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-37179

fun test() {
    fun Receiver.<!EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE!>foo<!>() {}
}

object Receiver {
    fun Receiver.<!EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE!>foo<!>() {}

    val Receiver.foo get() = Property
}

object Property {
    operator fun invoke() {}
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, getter, localFunction, objectDeclaration, operator,
propertyDeclaration, propertyWithExtensionReceiver */
