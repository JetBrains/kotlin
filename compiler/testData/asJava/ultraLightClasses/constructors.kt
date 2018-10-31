
class TestConstructor private constructor(p: Int = 1)
class A(vararg a: Int, f: () -> Unit) {}

class B {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    constructor()
}