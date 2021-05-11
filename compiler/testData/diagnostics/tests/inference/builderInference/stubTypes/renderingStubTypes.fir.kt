// !LANGUAGE: +StableBuilderInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -DEPRECATION -EXPERIMENTAL_IS_NOT_ENABLED -UNUSED_VARIABLE
// WITH_RUNTIME

import kotlin.experimental.ExperimentalTypeInference

@UseExperimental(ExperimentalTypeInference::class)
fun <R> build(
    @BuilderInference block: TestInterface<R>.() -> Unit
): R = TODO()

class Inv<K>

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
    fun getInv(): Inv<R>
}

fun <U> id(x: U) = x

fun test() {
    val ret = build {
        emit("1")
        get()
        getInv()
        ""
    }
}
