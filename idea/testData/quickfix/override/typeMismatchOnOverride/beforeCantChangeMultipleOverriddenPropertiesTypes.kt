// "Change 'A.x' type to '(Int) -> Int'" "false"
// ACTION: Change 'C.x' type to '(String) -> Int'
// ERROR: <html>Return type is '(jet.Int) &rarr; jet.Int', which is not a subtype of overridden<br/><b>internal</b> <b>abstract</b> <b>val</b> x: (jet.String) &rarr; jet.Int <i>defined in</i> A</html>
trait A {
    val x: (String) -> Int
}

trait B {
    val x: (String) -> Any
}

trait C : A, B {
    override val x: (Int) -> Int<caret>
}