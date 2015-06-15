// "Change 'A.x' type to '(Int) -> Int'" "false"
// ACTION: Change 'C.x' type to '(String) -> Int'
// ERROR: <html>Return type is '(kotlin.Int) &rarr; kotlin.Int', which is not a subtype of overridden<br/><b>internal</b> <b>abstract</b> <b>val</b> x: (kotlin.String) &rarr; kotlin.Int <i>defined in</i> A</html>
interface A {
    val x: (String) -> Int
}

interface B {
    val x: (String) -> Any
}

interface C : A, B {
    override val x: (Int) -> Int<caret>
}