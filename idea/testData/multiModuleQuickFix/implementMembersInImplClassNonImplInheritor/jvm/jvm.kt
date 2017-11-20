// "Implement members" "true"
// DISABLE-ERRORS

abstract actual class Bar {
    abstract actual fun foo()
}

class <caret>X : Bar()