// !DIAGNOSTICS: -UNUSED_PARAMETER -DEPRECATION -EXPERIMENTAL_IS_NOT_ENABLED -UNUSED_VARIABLE
// WITH_RUNTIME

import kotlin.experimental.ExperimentalTypeInference

@UseExperimental(ExperimentalTypeInference::class)
fun <R> combined(
    check: () -> Unit,
    @BuilderInference block: TestInterface<R>.() -> Unit
): R = TODO()

interface TestInterface<R> {
    fun emit(r: R)
}

fun test() {
    val ret = combined({ }) {
        <!INAPPLICABLE_CANDIDATE!>emit<!>(1)
    }
}
