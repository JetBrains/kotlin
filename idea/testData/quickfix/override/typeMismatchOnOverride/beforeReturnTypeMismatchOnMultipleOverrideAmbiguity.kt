// "Change 'B.foo' function return type to 'Int'" "false"
// "Change 'B.foo' function return type to 'Long'" "false"
// "Remove explicitly specified return type" "false"
// ERROR: <html>Return type is 'kotlin.String', which is not a subtype of overridden<br/><b>internal</b> <b>abstract</b> <b>fun</b> foo(): kotlin.Int <i>defined in</i> A</html>
abstract class A {
    abstract fun foo() : Int;
}

trait X {
    fun foo() : Long;
}

abstract class B : A(), X {
    abstract override fun foo() : String<caret>
}
