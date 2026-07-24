annotation class Annotation(vararg val strings: String)
annotation class AnnotationArray(vararg val value: Annotation)

@AnnotationArray(Annotation(strings = ["[sar]1", "[sar]2"]))
class CA
