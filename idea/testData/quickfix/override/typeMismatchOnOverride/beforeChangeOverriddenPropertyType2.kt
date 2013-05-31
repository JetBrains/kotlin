// "Change 'A.x' type to 'String'" "true"
trait A {
    var x: Int
}

trait B {
    var x: String
}

trait C : A, B {
    override var x: String<caret>
}