// !DIAGNOSTICS: -UNUSED_PARAMETER

object T3 {
    interface Foo<T>

    fun <T> delegate(): Foo<T> = TODO()

    operator fun <T> Foo<T>.provideDelegate(host: T3, p: Any?): Foo<T> = TODO()
    operator fun <T> Foo<T>.getValue(receiver: T3, p: Any?): T = TODO()

    val test1: String by <!TYPE_MISMATCH!>delegate()<!>
}
