// FIR_IDENTICAL
sealed class A {
    class B:A()
    class<!SYNTAX!><!> :A()
    fun test(a : A) {
        <!NON_EXHAUSTIVE_WHEN_STATEMENT("sealed class/interface; 'is B', 'is <no name provided>' branches or 'else' branch instead")!>when<!>(a) {
        }
    }
}