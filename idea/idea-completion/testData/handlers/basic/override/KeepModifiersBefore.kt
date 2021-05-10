// FIR_COMPARISON
class A {
    @Deprecated("") // it is deprecated
    public o<caret>
}

// ELEMENT_TEXT: "override fun equals(other: Any?): Boolean {...}"
