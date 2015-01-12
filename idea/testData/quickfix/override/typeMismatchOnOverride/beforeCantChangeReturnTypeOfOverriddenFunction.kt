// "class org.jetbrains.kotlin.idea.quickfix.ChangeFunctionReturnTypeFix" "false"
// ERROR: <html>Return type is 'kotlin.Long', which is not a subtype of overridden<br/><b>internal</b> <b>abstract</b> <b>fun</b> foo(): kotlin.Int <i>defined in</i> A</html>
trait A {
    fun foo(): Int
}

trait B {
    fun foo(): String
}

trait C : A, B {
    override fun foo(): Long
}
