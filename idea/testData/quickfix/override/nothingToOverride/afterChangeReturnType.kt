// "Change function signature to 'fun f(a: Int): Int'" "true"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Int</td></tr><tr><td>Found:</td><td>kotlin.String</td></tr></table></html>
open class A {
    open fun f(a: Int): Int = 0
}

class B : A(){
    // Note that when parameter types match, RETURN_TYPE_MISMATCH_ON_OVERRIDE error is reported
    // and "Change function signature" quickfix is not present.
    <caret>override fun f(a: Int): Int = "FOO"
}
