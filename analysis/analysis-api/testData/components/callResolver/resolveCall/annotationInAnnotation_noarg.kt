annotation class Annotation(vararg val strings: String)

annotation class AnnotationInner(val value: Annotation)

@AnnotationInner(<expr>Annotation()</expr>)
class C
