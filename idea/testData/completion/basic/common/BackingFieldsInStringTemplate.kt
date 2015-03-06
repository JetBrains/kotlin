class C {
    var property1 = "abc"
        get() = $property1 + 1
    var property2 = "abc"
        get() = $property2 + 1

    fun foo(){
        "${$<caret>}"
    }
}

// EXIST: $property1
// EXIST: $property2
// ABSENT: property1
// ABSENT: property2
