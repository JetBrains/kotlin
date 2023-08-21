// !LANGUAGE: +UnrestrictedBuilderInference
// WITH_STDLIB

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
fun <R> build(block: TestInterface<R>.() -> Unit) {}

interface TestInterface<R> {
    fun emit(r: R)
    fun getIn(): Inv<in R>
}

class Inv<T>

fun <K> capture(x: Inv<K>): K = null as K

fun box(): String {
    build {
        // String <: R
        emit("")
        // x = Capture(in R) [R <: x <: Any?]
        // Inv<x> <: Inv<K> => x == K
        // x <: K => R <: K => String <: K
        // K <: x => (???) K <: lowerBound(x) => K <: R => x <: R
        //
        // R = String
        // x <: String --> fail
        capture(getIn())
        ""
    }

    return "OK"
}