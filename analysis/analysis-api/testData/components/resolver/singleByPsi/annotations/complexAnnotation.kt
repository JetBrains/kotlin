annotation class Annotation(vararg val strings: String)
annotation class AnnotationArray(vararg val value: Annotation)

@AnnotationArray(Annot<caret>ation(strings = ["[sar]1", "[sar]2"]))
class CA
