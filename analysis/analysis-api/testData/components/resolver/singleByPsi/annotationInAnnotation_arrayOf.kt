annotation class Annotation(vararg val strings: String)

annotation class AnnotationInner(val value: Annotation)

@AnnotationInner(<expr>Annotation(strings = arrayOf("v1", "v2"))</expr>)
class C
