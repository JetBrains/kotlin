// "Change type to 'Int'" "true"
interface X {
    val x: Int
}

class A : X {
    override val x: Number<caret> = 42
}
