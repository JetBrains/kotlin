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
    val ret = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>combined<!>({ }) {
        emit(1)
    }
}
