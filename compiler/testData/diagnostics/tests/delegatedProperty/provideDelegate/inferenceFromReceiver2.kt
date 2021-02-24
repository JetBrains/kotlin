// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

object Inference2 {
    interface Foo<T>

    fun <T> delegate(): Foo<T> = TODO()

    operator fun <T> Foo<T>.provideDelegate(host: T, p: Any?): Foo<T> = TODO()
    operator fun <T> Foo<T>.getValue(receiver: Inference2, p: Any?): String = TODO()

    val test1: String by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE{NI}!><!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>delegate<!>()<!> // same story like in Inference1
    val test2: String by delegate<Inference2>()
}
