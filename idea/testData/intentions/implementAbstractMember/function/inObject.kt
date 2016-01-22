// IS_APPLICABLE: false
// ERROR: Abstract function 'foo' in non-abstract class 'A'
object A {
    abstract fun <caret>foo(): Int
}