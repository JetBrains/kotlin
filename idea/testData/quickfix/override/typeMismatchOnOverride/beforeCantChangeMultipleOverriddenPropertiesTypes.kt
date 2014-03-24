// "Change 'A.x' type to '(Int) -> Int'" "false"
// ACTION: Change 'C.x' type to '(String) -> Int'
// ACTION: Disable inspection
// ACTION: Edit inspection profile setting
// ERROR: <html>Return type is '(kotlin.Int) &rarr; kotlin.Int', which is not a subtype of overridden<br/><b>internal</b> <b>abstract</b> <b>val</b> x: (kotlin.String) &rarr; kotlin.Int <i>defined in</i> A</html>
trait A {
    val x: (String) -> Int
}

trait B {
    val x: (String) -> Any
}

trait C : A, B {
    override val x: (Int) -> Int<caret>
}