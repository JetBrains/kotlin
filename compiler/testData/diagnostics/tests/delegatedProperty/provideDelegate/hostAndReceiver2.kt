// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

object T2 {
    interface Foo<T>

    fun <T> delegate(): Foo<T> = TODO()

    operator fun <T> Foo<T>.provideDelegate(host: T2, p: Any?): Foo<T> = TODO()
    operator fun <T> Foo<T>.getValue(receiver: String, p: Any?): T = TODO()

    val String.test1: String by delegate()
    val test2: String by <!NI;DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!><!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>delegate<!>()<!>
}