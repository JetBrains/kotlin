// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
package some

import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
fun <T> applyBI(@BuilderInference t: T): T = t

fun <V> myBuildList(a: MutableList<out V>.() -> Unit) {}

fun main() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>myBuildList<!>(<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>applyBI<!> {
        this.<!DEBUG_INFO_MISSING_UNRESOLVED!>add<!>("1")
    })
}