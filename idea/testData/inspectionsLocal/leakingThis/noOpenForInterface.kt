// PROBLEM: Calling non-final function foo in constructor

interface My {
    fun foo() {}
}

open class Your : My {

    init {
        // exactly one fix action should be here (make 'Your' final)
        // making 'foo' final will provoke an error
        <caret>foo()
    }
}
