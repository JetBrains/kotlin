// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_FIR_DIAGNOSTICS
// ISSUE: KT-58549

fun main() {
    val x: kotlin.<!UNRESOLVED_REFERENCE!>Cloneable<!> = if (true) intArrayOf(1) else longArrayOf(1)
    x
}
