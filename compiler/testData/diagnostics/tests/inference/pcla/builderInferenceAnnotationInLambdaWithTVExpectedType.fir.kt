// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
package some

import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
fun <T> applyBI(@BuilderInference t: T): T = t

fun <V> myBuildList(a: MutableList<out V>.() -> Unit) {}

fun main() {
    <!CANNOT_INFER_PARAMETER_TYPE!>myBuildList<!>(<!CANNOT_INFER_PARAMETER_TYPE!>applyBI<!> <!CANNOT_INFER_IT_PARAMETER_TYPE!>{
        <!CANNOT_INFER_RECEIVER_PARAMETER_TYPE!>this<!>.<!UNRESOLVED_REFERENCE!>add<!>("1")
    }<!>)
}
