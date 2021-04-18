// FIR_IDENTICAL
interface T {
    fun foo()
    fun bar()
}

class C(t :T) : T by t {
    <caret>
}

// KT-5103