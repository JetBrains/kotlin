// "Make 'A' abstract" "false"
// ERROR: <html>Class 'X' must override <b>public</b> <b>open</b> <b>fun</b> foo(): kotlin.Unit <i>defined in</i> X<br />because it inherits many implementations of it</html>
// ACTION: Create Test
// ACTION: Make internal
// ACTION: Make private
// ACTION: Move 'X' to separate file

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