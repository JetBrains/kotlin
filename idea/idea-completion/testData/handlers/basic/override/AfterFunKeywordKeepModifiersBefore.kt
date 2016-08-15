class A {
    @Deprecated("") // it is deprecated
    public override fun e<caret>
}

// ELEMENT_TEXT: "override operator fun equals(other: Any?): Boolean {...}"
