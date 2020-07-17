// "Replace with 'Bar(i)'" "true"
class Test {
    @get:<caret>Foo(1)
    val s: String = ""
}

@Deprecated("Replace with Bar", ReplaceWith("Bar(i)"))
@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class Foo(val i: Int)

@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class Bar(val i: Int)