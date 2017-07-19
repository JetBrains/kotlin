@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ParameterAnnotation(val a: Int = 0)

class TestClass(private vararg @ParameterAnnotation(42) val <caret>words: String = arrayOf())