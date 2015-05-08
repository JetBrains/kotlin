// "Cast expression 'Foo<Number>()' to 'Foo<Int>'" "false"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>Foo&lt;kotlin.Int&gt;</td></tr><tr><td>Found:</td><td>Foo&lt;kotlin.Number&gt;</td></tr></table></html>
class Foo<T>

fun foo(): Foo<Int> {
    return Foo<Number>()
}