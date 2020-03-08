// "Replace with 'Bar()'" "true"
class Test {
    @get:<caret>Foo
    val s: String = ""
}

@Deprecated("Replace with Bar", ReplaceWith("Bar()"))
@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class Foo

@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class Bar