class C {
    var property = "abc"
        get() = $property + 1

    fun foo() {
        this.<caret>
    }
}

// EXIST: $property
