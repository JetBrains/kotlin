annotation class Annotation(vararg val strings: String)

annotation class AnnotationArray(vararg val annos: Annotation)

@AnnotationArray(annos = <expr>[Annotation("v1", "v2"), Annotation(strings = ["v3", "v4"])]</expr>)
class C
