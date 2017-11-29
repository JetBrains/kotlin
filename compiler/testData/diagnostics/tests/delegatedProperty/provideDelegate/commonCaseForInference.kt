// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

object CommonCase {
    interface Fas<D, E, R>

    fun <D, E, R> delegate() : Fas<D, E, R> = TODO()

    operator fun <D, E, R> Fas<D, E, R>.provideDelegate(host: D, p: Any?): Fas<D, E, R> = TODO()
    operator fun <D, E, R> Fas<D, E, R>.getValue(receiver: E, p: Any?): R = TODO()

    val Long.test1: String by <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>delegate<!>() // common test, not working because of Inference1
    val Long.test2: String by delegate<CommonCase, Long, String>() // should work
}