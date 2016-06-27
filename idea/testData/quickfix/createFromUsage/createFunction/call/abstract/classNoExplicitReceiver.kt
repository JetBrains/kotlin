// "Create abstract function 'foo'" "true"
abstract class A {
    fun bar(b: Boolean) {}

    fun test() {
        bar(<caret>foo(1, "2"))
    }
}