// "Create member property 'foo'" "true"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Int</td></tr><tr><td>Found:</td><td>A&lt;kotlin.Int&gt;</td></tr></table></html>
// ERROR: Property must be initialized or be abstract

class A<T>(val n: T)

fun test(): Int {
    return A(1).<caret>foo as A<Int>
}
