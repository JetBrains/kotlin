// "Create abstract function 'bar'" "true"
// ERROR: Class 'Foo' is not abstract and does not implement abstract member public abstract fun bar(): Unit defined in I

interface I

class Foo : I {
    fun foo() {
        <caret>bar()
    }
}