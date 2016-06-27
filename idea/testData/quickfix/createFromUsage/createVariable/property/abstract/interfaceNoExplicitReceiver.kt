// "Create abstract property 'foo'" "true"
interface A {
    fun bar(b: Boolean) {}

    fun test() {
        bar(<caret>foo)
    }
}