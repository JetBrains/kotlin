// WITH_STDLIB

import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
fun <E1> buildL(x: MutableList<E1>.() -> Int): Int = 1
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
fun <E2> buildL(x: MutableList<E2>.() -> String): String = ""

fun main() {
    <!NONE_APPLICABLE!>buildL<!> {
        <!UNRESOLVED_REFERENCE!>add<!>("")
        ""
    }
}
