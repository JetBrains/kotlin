// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// FILE: Extensions.kt
package libPackage

private infix operator fun CharSequence.contains(regex: Regex): Boolean {
    println("my contains")
    return true
}

// FILE: TestCase2.kt
package sentence3
import libPackage.* //nothing to import, extension is private

// TESTCASE NUMBER: 1
fun case1() {
    val regex = Regex("")
    "" <!INFIX_MODIFIER_REQUIRED!>contains<!> regex
}
