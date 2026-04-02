// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-37179

fun test() {
    fun Receiver.foo() {}
}

object Receiver {
    fun Receiver.foo() {}

    val Receiver.foo get() = Property
}

object Property {
    operator fun invoke() {}
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, getter, localFunction, objectDeclaration, operator,
propertyDeclaration, propertyWithExtensionReceiver */
