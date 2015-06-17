class MyClassA
deprecated class MyClassB
class MyClassC

fun foo(myCla<caret>)

// ORDER: myClassA
// ORDER: myClassC
// ORDER: myClassB
