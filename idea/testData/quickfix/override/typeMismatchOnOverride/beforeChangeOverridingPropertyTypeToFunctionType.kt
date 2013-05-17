// "Change 'B.x' type to '(String) -> Int'" "true"
trait A {
    var x: (String) -> Int
}
trait B : A {
    override var x: (Int) -> String<caret>
}