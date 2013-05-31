// "Change 'A.x' type to 'Int'" "true"
trait X {
    val x: Int
}

class A : X {
    override val x: Number<caret> = 42
}