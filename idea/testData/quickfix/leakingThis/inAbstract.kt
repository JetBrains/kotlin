// "Make 'My' final" "false"
// ACTION: Add 'my =' to argument

abstract class My {
    init {
        register(<caret>this)
    }
}

fun register(my: My) {}