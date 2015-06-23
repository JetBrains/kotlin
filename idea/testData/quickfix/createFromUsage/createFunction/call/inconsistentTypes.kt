// "Create member function 'foo'" "true"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Int</td></tr><tr><td>Found:</td><td>A&lt;kotlin.Int&gt;</td></tr></table></html>

class A<T>(val n: T)

fun test(): Int {
    return A(1).<caret>foo("s", 1) as A<Int>
}