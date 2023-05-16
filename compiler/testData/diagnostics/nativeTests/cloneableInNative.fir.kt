// ISSUE: KT-58549

fun main() {
    val x: <!UNRESOLVED_REFERENCE!>kotlin.Cloneable<!> = if (true) intArrayOf(1) else longArrayOf(1)
    x
}
