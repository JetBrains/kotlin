// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

object Inference1 {
    interface Foo<T>

    fun <T> delegate(): Foo<T> = TODO()

    operator fun <T> Foo<T>.getValue(receiver: T, p: Any?): String = TODO()

    // not working because resulting descriptor for getValue contains type `???` instead of `T`
    val test1: String by <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>delegate<!>()

    val test2: String by delegate<Inference1>()
}
