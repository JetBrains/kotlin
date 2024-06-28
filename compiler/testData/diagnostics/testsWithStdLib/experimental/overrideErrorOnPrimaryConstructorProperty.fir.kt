// ISSUE: KT-37786

@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY)
annotation class Experimental

interface Foo {
    @Experimental
    val foo: Int
}

data class Bar @Experimental constructor(override val <!OPT_IN_OVERRIDE_ERROR!>foo<!>: Int): Foo

fun main() {
    @OptIn(Experimental::class)
    val bar = Bar(42)

    bar.foo
}
