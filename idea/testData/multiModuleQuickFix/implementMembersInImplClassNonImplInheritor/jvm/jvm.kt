// "Implement members" "true"
// DISABLE-ERRORS

abstract impl class Bar {
    abstract impl fun foo()
}

class <caret>X : Bar()