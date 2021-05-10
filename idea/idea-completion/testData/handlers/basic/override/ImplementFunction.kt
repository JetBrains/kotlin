// FIR_COMPARISON
interface I {
    fun foo(p: Int)
}

class A : I {
    o<caret>
}

// ELEMENT_TEXT: "override fun foo(p: Int) {...}"
