class C {
    var property = "abc"
        get() = $property + 1
}


fun foo(c: C){
    c.<caret>
}

// ABSENT: $property
