// FIR_IDENTICAL
open class A {
    open fun foo(`object` : Any): Int = 0
}

class C : A() {
    <caret>
}
