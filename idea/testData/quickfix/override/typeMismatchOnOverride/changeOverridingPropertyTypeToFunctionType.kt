// "Change type to '(String) -> Int'" "true"
interface A {
    var x: (String) -> Int
}
interface B : A {
    override var x: (Int) -> String<caret>
}
