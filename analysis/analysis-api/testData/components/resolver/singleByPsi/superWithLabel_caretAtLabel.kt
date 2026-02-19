interface A {
    fun a() {}
}
class Foo : A {
    fun foo() {
        super@F<caret>oo.a()
    }
}