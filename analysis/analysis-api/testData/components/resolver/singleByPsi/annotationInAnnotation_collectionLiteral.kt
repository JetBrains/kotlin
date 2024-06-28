annotation class Annotation(vararg val strings: String)

annotation class AnnotationInner(val value: Annotation)

@AnnotationInner(<expr>Annotation(["v1", "v2"])</expr>)
class C
