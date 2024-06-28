// FIR_IDENTICAL
package test

annotation class Ann(val s1: String, val s2: String)

val i = 1

@Ann(s1 = "a" + "b", s2 = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"a" + "a$i"<!>) class MyClass

// EXPECTED: @Ann(s1 = "ab", s2 = "aa1")
