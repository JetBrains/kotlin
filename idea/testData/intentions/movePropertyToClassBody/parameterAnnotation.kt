
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ParameterAnnotation(val a: Int = 0)

class TestClass(private @ParameterAnnotation(42) val <caret>text: String = "LoremIpsum", val flag: Boolean)