// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-41085
// LANGUAGE: +MultiPlatformProjects

// KT-41085: Performance problem in ExpectActualDeclarationChecker.checkActualDeclarationHasExpected
// The check is quadratic: for each declaration it checks all potentially compatible declarations.
// Having many private top-level declarations with the same name triggers the issue.

// MODULE: common
expect fun testFun(): String

// MODULE: main(common)
actual fun <!ACTUAL_WITHOUT_EXPECT!>testFun<!>(): String = "test"

// Multiple private overloads with the same name - this triggers the quadratic behavior
// when the checker iterates over all declarations looking for expect/actual matches
private fun foo() {}
private fun foo(x: Int): Int = x
private fun foo(x: String): String = x
private fun foo(x: Double): Double = x
private fun foo(x: Long): Long = x
private fun foo(x: Boolean): Boolean = x
private fun foo(x: Int, y: Int): Int = x + y
private fun foo(x: Int, y: String): String = "$x$y"
private fun foo(x: String, y: String): String = x + y
private fun foo(x: List<Int>): List<Int> = x

/* GENERATED_FIR_TAGS: actual, additiveExpression, expect, functionDeclaration, stringLiteral */
