package test

annotation class Ann(val s1: String)

@Ann(s1 = "\$ab") class MyClass

// EXPECTED: @Ann(s1 = "$ab")
