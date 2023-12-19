// !LANGUAGE: +UnrestrictedBuilderInference
// WITH_STDLIB
// ISSUE: KT-61250
// IGNORE_BACKEND_K2: ANY

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
fun <R> build(block: TestInterface<R>.() -> Unit) {}

@OptIn(ExperimentalTypeInference::class)
fun <R> build2(block: TestInterface<R>.() -> Unit) {}

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

fun box(): String {
    build {
        emit("")
        getInv()
        captureOut(getInv())
        captureIn(getInv())

        capture(getOut())
        ""
    }
    build {
        emit("")
        capture(getIn())
        ""
    }

    return "OK"
}
