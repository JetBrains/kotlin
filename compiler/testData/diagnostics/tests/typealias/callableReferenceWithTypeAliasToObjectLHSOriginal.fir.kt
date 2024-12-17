// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-73608

sealed class Meter {
    fun compute(pattern: Pattern): Double {
        return 1.0
    }
}

data object MeterA : Meter()

class Pattern()

typealias Meter1 = MeterA

// Changing Meter1 to MeterA "fixes" the compilation problem
val cmds: Map<String, (Pattern) -> Number> = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>mapOf("" to Meter1::compute)<!>

fun main() {
    println(cmds.entries.last().value(Pattern()))
}
