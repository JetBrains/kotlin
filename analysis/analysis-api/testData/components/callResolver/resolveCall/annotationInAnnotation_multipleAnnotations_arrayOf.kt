annotation class Annotation(vararg val strings: String)

annotation class AnnotationArray(vararg val annos: Annotation)

@AnnotationArray(<expr>annos = arrayOf(Annotation("v1", "v2"), Annotation(strings = arrayOf("v3", "v4")))</expr>)
class C
