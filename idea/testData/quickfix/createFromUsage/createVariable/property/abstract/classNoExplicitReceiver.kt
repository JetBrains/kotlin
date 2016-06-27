// "Create abstract property 'foo'" "true"
abstract class A {
    fun bar(b: Boolean) {}

    fun test() {
        bar(<caret>foo)
    }
}