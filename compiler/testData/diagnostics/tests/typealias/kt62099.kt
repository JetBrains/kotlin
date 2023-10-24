// FIR_IDENTICAL
// ISSUE: KT-62099

abstract class Foo<T> {
    inner class Inner
    abstract fun render(context: Inner)
}

typealias TA = Foo<String>

class Test : TA() {
    override fun render(context: Inner) {}
}

typealias TA2<T> = Foo<T>

class Test2 : TA2<String>() {
    override fun render(context: Inner) {}
}

typealias TA3<T> = Foo<T>
typealias TA3_2<T> = TA3<T>

class Test3 : TA3_2<String>() {
    override fun render(context: Inner) {}
}

typealias TA4<T> = Foo<T>
typealias TA4_2 = TA4<String>

class Test4 : TA4_2() {
    override fun render(context: Inner) {}
}
