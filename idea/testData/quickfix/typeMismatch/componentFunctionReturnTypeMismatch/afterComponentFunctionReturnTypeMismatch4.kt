// "Change 'A.component2' function return type to 'Unit'" "true"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>jet.Unit</td></tr><tr><td>Found:</td><td>jet.Int</td></tr></table></html>
abstract class A {
    abstract fun component1(): Int
    fun component2(): Unit = 42
}

fun foo(a: A) {
    val (w: Int, x: Unit) = a<caret>
}