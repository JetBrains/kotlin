
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class PropertyAnnotation(val a: Int = 0)

class TestClass(private @PropertyAnnotation(42) val <caret>text: String = "LoremIpsum")