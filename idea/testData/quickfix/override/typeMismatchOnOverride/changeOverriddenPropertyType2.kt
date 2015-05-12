// "Change 'A.x' type to 'String'" "true"
interface A {
    var x: Int
}

interface B {
    var x: String
}

interface C : A, B {
    override var x: String<caret>
}