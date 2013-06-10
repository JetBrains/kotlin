// "Change 'A.foo' function return type to 'Long'" "false"
// ERROR: <html>Return type is 'jet.Long', which is not a subtype of overridden<br/><b>internal</b> <b>abstract</b> <b>fun</b> foo(): jet.Int <i>defined in</i> A</html>
trait A {
    fun foo(): Int
}

trait B {
    fun foo(): String
}

trait C : A, B {
    override fun foo(): Long
}