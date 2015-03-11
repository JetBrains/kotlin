class C {
    var property = "abc"
        get() = $property + 1

    fun foo(){
        "$<caret>"
    }
}

// ELEMENT: $property
