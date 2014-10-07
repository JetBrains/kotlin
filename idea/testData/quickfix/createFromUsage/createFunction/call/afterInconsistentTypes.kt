// "Create function 'foo' from usage" "true"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Int</td></tr><tr><td>Found:</td><td>A&lt;kotlin.Int&gt;</td></tr></table></html>

class A<T>(val n: T) {
    fun foo(s: String, arg: T): Any {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun test(): Int {
    return A(1).foo("s", 1) as A<Int>
}