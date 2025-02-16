// QUERY: annotations

context(@MyAnno("1" + "2") vari<caret>able: String)
fun foo() = 1

annotation class MyAnno(val v: String)