// "Change 'B.x' type to '(Int) -> Int'" "true"
interface A {
    val x: (Int) -> Int
}

interface B {
    val x: (String) -> Any
}

interface C : A, B {
    override val x: (Int) -> Int<caret>
}