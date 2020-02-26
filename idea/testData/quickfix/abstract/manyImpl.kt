// "Make 'A' 'abstract'" "false"
// ERROR: Class 'X' must override public open fun foo(): Unit defined in X because it inherits many implementations of it
// ACTION: Create test
// ACTION: Make internal
// ACTION: Make private
// ACTION: Extract 'X' from current file
// ACTION: Implement members

interface D {
    fun foo()
}

interface E {
    fun foo() {}
}

object Impl : D, E {
    override fun foo() {}
}

<caret>class X : D by Impl, E by Impl {}