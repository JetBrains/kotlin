package test

annotation class Ann(val s1: String, val s2: String)

val i = 1

@Ann(s1 = "a" + "b", s2 = "a" + "a$i") class MyClass

// EXPECTED: @Ann(s1 = "ab", s2 = "aa1")
