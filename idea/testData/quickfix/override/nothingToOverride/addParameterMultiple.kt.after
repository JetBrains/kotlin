// "Change function signature..." "true"
// ERROR: <html>Class 'B' must be declared abstract or implement abstract member<br/><b>internal</b> <b>abstract</b> <b>fun</b> f(a: kotlin.String): kotlin.Unit <i>defined in</i> A</html>
trait A {
    fun f(a: Int)
    fun f(a: String)
}

class B : A {
    <caret>override fun f(a: Int) {}
}
