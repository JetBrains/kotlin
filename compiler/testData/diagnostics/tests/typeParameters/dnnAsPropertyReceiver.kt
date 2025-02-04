// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74929

val <T : Number?> (T & Any).prop : T get() = this

interface Base {
    val <T : Number?> (T & Any).prop : T
}
