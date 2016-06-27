// "Create abstract function 'foo'" "true"
abstract class A {
    fun bar(b: Boolean) {}

    fun test(a: A) {
        bar(a.<caret>foo(1, "2"))
    }
}