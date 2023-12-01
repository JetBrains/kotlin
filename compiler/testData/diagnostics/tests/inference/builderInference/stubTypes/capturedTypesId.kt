// !LANGUAGE: +UnrestrictedBuilderInference
// !DIAGNOSTICS: -DEPRECATION -UNCHECKED_CAST -OPT_IN_IS_NOT_ENABLED
// WITH_STDLIB
// ISSUE: KT-63996

// FILE: main.kt

fun <R> build(block: TestInterface<R>.() -> Unit): R = TODO()

interface TestInterface<R> {
    fun emit(r: R)
    fun getOut(): Inv<out R>
}

class Inv<T> {
}

fun <K> capture(x: Inv<K>): K = null as K
fun <I> id(x: I): I = null as I

fun main() {
    build {
        emit("")
        // K is fixed into CapturedType(out NotFixed: TypeVariable(R)
        capture(id(<!TYPE_MISMATCH, TYPE_MISMATCH!>getOut()<!>)) // unexpected TYPE_MISMATCH (KT-63996)
        // capture(getOut()) // OK!!!
        Unit
    }
    build<String> {
        emit("")
        // K is fixed into CapturedType(out NotFixed: TypeVariable(R)
        capture(id(getOut())) // OK!!!
        // capture(getOut()) // OK!!!
        Unit
    }
}
