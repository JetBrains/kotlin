// "Change 'A.x' type to '(Int) -> Int'" "false"
// ACTION: Change 'C.x' type to '(String) -> Int'
// ERROR: Return type of 'x' is not a subtype of the return type of the overridden member 'public abstract val x: (kotlin.String) -> kotlin.Int defined in A'
interface A {
    val x: (String) -> Int
}

interface B {
    val x: (String) -> Any
}

interface C : A, B {
    override val x: (Int) -> Int<caret>
}