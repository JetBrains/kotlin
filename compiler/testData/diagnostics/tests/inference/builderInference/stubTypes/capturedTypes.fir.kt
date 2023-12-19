// !LANGUAGE: +UnrestrictedBuilderInference
// !DIAGNOSTICS: -DEPRECATION -UNCHECKED_CAST -OPT_IN_IS_NOT_ENABLED
// WITH_STDLIB
// ISSUE: KT-61250 (for K2/PCLA difference)

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
fun <R> build(block: TestInterface<R>.() -> Unit): R = TODO()

@OptIn(ExperimentalTypeInference::class)
fun <R> build2(block: TestInterface<R>.() -> Unit): R = TODO()

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
    fun getInv(): Inv<R>
    fun getOut(): Inv<out R>
    fun getIn(): Inv<in R>
}

class Inv<T>

fun <K> captureOut(x: Inv<out K>): K = null as K
fun <K> captureIn(x: Inv<out K>): K = null as K
fun <K> capture(x: Inv<K>): K = null as K

fun main() {
    build {
        emit("")
        getInv()
        captureOut(getInv())
        captureIn(getInv())

        // K is fixed into CapturedType(out NotFixed: TypeVariable(R))
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>capture<!>(<!ARGUMENT_TYPE_MISMATCH!>getOut()<!>)
        ""
    }
    build {
        emit("")
        // K is fixed into CapturedType(in NotFixed: TypeVariable(R))
        capture(getIn())
        ""
    }
}
