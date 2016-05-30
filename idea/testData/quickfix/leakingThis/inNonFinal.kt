// "Make 'My' final" "true"

open class My {
    init {
        register(<caret>this)
    }
}

fun register(my: My) {}