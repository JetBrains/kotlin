// RUN_PIPELINE_TILL: SOURCE
// ISSUE: KT-54874

fun main(args: Array<String>) {
    val comparator = <!NO_COMPANION_OBJECT!>Comparator<Long?><!>
}