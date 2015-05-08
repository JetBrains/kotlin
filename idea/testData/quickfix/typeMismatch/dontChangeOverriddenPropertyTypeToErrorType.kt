// "Change 'B.x' type to '(String) -> [ERROR : Ay]'" "false"
// ACTION: Change 'A.x' type to '(Int) -> Int'
// ACTION: Disable inspection
// ACTION: Edit inspection profile setting
// ERROR: <html>Return type is '(kotlin.Int) &rarr; kotlin.Int', which is not a subtype of overridden<br/><b>internal</b> <b>abstract</b> <b>val</b> x: (kotlin.String) &rarr; [ERROR : Ay] <i>defined in</i> A</html>
// ERROR: Unresolved reference: Ay
trait A {
    val x: (String) -> Ay
}
trait B : A {
    override val x: (Int) -> Int<caret>
}