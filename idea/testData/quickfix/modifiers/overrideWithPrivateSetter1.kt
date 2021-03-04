// "Make 'bar' 'final'" "true"
interface Foo {
    val bar: String
}

open class FooImpl : Foo {
    override var bar: String = ""
        <caret>private set
}
/* FIR_COMPARISON */
