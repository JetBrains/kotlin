// "Change 'B.x' type to '(String) -> [ERROR : Ay]'" "false"
// ACTION: Change 'A.x' type to '(Int) -> Int'
// ERROR: <html>Return type is '(kotlin.Int) &rarr; kotlin.Int', which is not a subtype of overridden<br/><b>internal</b> <b>abstract</b> <b>val</b> x: (kotlin.String) &rarr; [ERROR : Ay] <i>defined in</i> A</html>
// ERROR: Unresolved reference: Ay
interface A {
    val x: (String) -> Ay
}
interface B : A {
    override val x: (Int) -> Int<caret>
}