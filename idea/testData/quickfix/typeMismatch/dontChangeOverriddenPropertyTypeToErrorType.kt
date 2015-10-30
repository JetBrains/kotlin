// "Change 'B.x' type to '(String) -> [ERROR : Ay]'" "false"
// ACTION: Change 'A.x' type to '(Int) -> Int'
// ERROR: Return type of 'x' is not a subtype of the return type of the overridden member 'public abstract val x: (kotlin.String) -> [ERROR : Ay] defined in A'
// ERROR: Unresolved reference: Ay
interface A {
    val x: (String) -> Ay
}
interface B : A {
    override val x: (Int) -> Int<caret>
}