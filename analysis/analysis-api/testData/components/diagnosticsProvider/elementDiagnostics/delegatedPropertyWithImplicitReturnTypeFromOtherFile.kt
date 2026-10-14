// ISSUE: KT-89635
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDotQualifiedExpression
// FILE: Intf.kt
interface Intf<X> {
    fun get(): X
    val prop get() = get()
}

// FILE: main.kt
abstract class Wrap<A>(delegate: Intf<A>) : Intf<A> by delegate

fun <B> useWrap(w: Wrap<B>): B = w.<caret>prop
