// "Create class 'Foo'" "true"
open class A
interface I

fun <T : I> foo() where T : A {}

fun x() {
    foo<<caret>Foo>()
}