// FIR_IDENTICAL
// COPY_DOC
abstract class A {
    /**
     * @see TEST
     */
    abstract fun foo()
}

class B : A() {
    <caret>
}