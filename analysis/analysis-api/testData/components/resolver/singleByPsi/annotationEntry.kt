annotation class Annotation(vararg val strings: String)

annotation class AnnotationInner(val value: Annotation)

<expr>@AnnotationInner(Annotation("v1", "v2"))</expr>
class C
