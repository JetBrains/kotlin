// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74929

val <<!TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>T : Number?<!>> (T & Any).prop : T get() = this

interface Base {
    val <<!TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>T : Number?<!>> (T & Any).prop : T
}
