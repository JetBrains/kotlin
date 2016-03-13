// "Change 'A.x' type to '(Int) -> Int'" "false"
// ACTION: Change 'C.x' type to '(String) -> Int'
// ERROR: Type of 'x' is not a subtype of the overridden property 'public abstract val x: (String) -> Int defined in A'
interface A {
    val x: (String) -> Int
}

interface B {
    val x: (String) -> Any
}

interface C : A, B {
    override val x: (Int) -> Int<caret>
}