// IS_APPLICABLE: false
// WITH_RUNTIME

interface A

interface B : A {
    val bar: Any?
}

class Foo {
    private val a: A = object : A {}

    val isB: Boolean
        get() = a.let<caret> { it is B && it.bar != null }
}