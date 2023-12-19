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
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.String>")!>getInv()<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>captureOut(<!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.String>")!>getInv()<!>)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>captureIn(<!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.String>")!>getInv()<!>)<!>

        // K is fixed into CapturedType(out NotFixed: TypeVariable(R))
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>capture(<!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.String>")!>getOut()<!>)<!>
        ""
    }
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>build<!> {
        emit("")
        // K is fixed into CapturedType(in NotFixed: TypeVariable(R))
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>capture(<!DEBUG_INFO_EXPRESSION_TYPE("Inv<in kotlin.Any?>")!>getIn()<!>)<!>
        ""
    }
}
