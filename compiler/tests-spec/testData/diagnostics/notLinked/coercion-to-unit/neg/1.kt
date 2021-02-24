/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: coercion-to-unit
 * NUMBER: 1
 * DESCRIPTION: Coercion to Unit error diagnostics absence
 * ISSUES: KT-38490
 */

// TESTCASE NUMBER: 1

val y0 = when (<!UNUSED_EXPRESSION!>2<!>) {
    else -> <!INVALID_IF_AS_EXPRESSION!>if<!> (true) {""}
}

val w:Any = TODO()

val y1 = when (2) {
    else -> if (true) {""} // false ok with coercion to Unit
}