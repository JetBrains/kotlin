// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER -DEPRECATION -OPT_IN_IS_NOT_ENABLED -UNUSED_VARIABLE
// WITH_STDLIB

import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
fun <R> combined(
    check: () -> Unit,
    @BuilderInference block: TestInterface<R>.() -> Unit
): R = TODO()

interface TestInterface<R> {
    fun emit(r: R)
}

fun test() {
    val ret = combined({ }) {
        emit(1)
    }
}
