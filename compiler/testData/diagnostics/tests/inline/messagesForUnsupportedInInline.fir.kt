inline fun inlineFun() {
    fun localFun() {}
    class LocalClass {}
}

fun outerFun() {
    inline fun localInlineFun() {}
}

abstract class Base {
    abstract fun withDefault(f: () -> Unit = { -> })
}

class Derived : Base() {
    override final inline fun withDefault(
            f: () -> Unit
    ) {}
}