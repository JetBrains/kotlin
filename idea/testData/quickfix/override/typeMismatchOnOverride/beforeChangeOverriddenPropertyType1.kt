// "Change 'B.x' type to '(Int) -> Int'" "true"
trait A {
    val x: (Int) -> Int
}

trait B {
    val x: (String) -> Any
}

trait C : A, B {
    override val x: (Int) -> Int<caret>
}