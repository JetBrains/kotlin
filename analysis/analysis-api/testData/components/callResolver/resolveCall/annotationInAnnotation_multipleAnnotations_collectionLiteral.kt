annotation class Annotation(vararg val strings: String)

annotation class AnnotationArray(vararg val annos: Annotation)

<expr>@AnnotationArray(annos = [Annotation("v1", "v2"), Annotation(["v3", "v4"])])</expr>
class C
