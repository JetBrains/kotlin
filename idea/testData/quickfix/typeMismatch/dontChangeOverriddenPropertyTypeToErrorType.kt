// "Change 'B.x' type to '(String) -> [ERROR : Ay]'" "false"
// ACTION: Change 'A.x' type to '(Int) -> Int'
// ERROR: Type of 'x' is not a subtype of the overridden property 'public abstract val x: (String) -> [ERROR : Ay] defined in A'
// ERROR: Unresolved reference: Ay
interface A {
    val x: (String) -> Ay
}
interface B : A {
    override val x: (Int) -> Int<caret>
}