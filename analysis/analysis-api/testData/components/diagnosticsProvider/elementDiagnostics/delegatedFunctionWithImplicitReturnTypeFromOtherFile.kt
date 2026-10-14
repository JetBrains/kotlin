// ISSUE: KT-89635
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDotQualifiedExpression
// FILE: Intf.kt
interface Intf<X> {
    fun id(x: X) = x
}

// FILE: main.kt
abstract class Wrap<A>(delegate: Intf<A>) : Intf<A> by delegate {
    abstract fun produce(): A
}

fun <B> useWrap(w: Wrap<B>): B = w.<caret>id(w.produce())
