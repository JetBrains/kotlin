// WITH_RUNTIME
// IS_APPLICABLE: false

class X {
    init {
        <caret>throw RuntimeException()
    }
}
