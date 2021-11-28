// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER -OPT_IN_IS_NOT_ENABLED
// WITH_STDLIB

import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
fun <T> foo(x: T, @BuilderInference builder: T.() -> Unit): Unit = TODO()

class Bar<T>

fun test() {
    foo(1) {
        <!UNRESOLVED_REFERENCE!>dsgfsdg<!>
    }
}
