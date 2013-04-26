// "Cast expression 'a: A' to 'B'" "false"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>B</td></tr><tr><td>Found:</td><td>A</td></tr></table></html>
// ACTION: Change 'foo' function return type to 'A'
open class A
class B : A()

fun foo(a: A): B {
    return a: A<caret>
}