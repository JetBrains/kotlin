// !DIAGNOSTICS: -UNUSED_PARAMETER -DEPRECATION -UNCHECKED_CAST -EXPERIMENTAL_IS_NOT_ENABLED -UNUSED_VARIABLE
// WITH_RUNTIME

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

@UseExperimental(ExperimentalTypeInference::class)
fun <R> build(@BuilderInference block: TestInterface<R>.() -> Unit): R = TODO()

@UseExperimental(ExperimentalTypeInference::class)
fun <R> build2(@BuilderInference block: TestInterface<R>.() -> Unit): R = TODO()

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
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<TypeVariable(R)>")!>getInv()<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R)")!>captureOut(<!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.String>")!>getInv()<!>)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R)")!>captureIn(<!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.String>")!>getInv()<!>)<!>

        // K is fixed into CapturedType(out NotFixed: TypeVariable(R))
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R)")!>capture(<!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.String>")!>getOut()<!>)<!>
        ""
    }
    build {
        emit("")
        // K is fixed into CapturedType(in NotFixed: TypeVariable(R))
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>capture(<!DEBUG_INFO_EXPRESSION_TYPE("Inv<in kotlin.Any?>")!>getIn()<!>)<!>
        ""
    }
}