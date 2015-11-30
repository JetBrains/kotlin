import constants.*

@AnnotationClass("$b $s $i $l $f $d $bb $c $str")
class DummyClass()

fun main(args: Array<String>) {
    val klass = javaClass<DummyClass>()!!
    val annotationClass = javaClass<AnnotationClass>()
    val annotation = klass.getAnnotation(annotationClass)!!
    val value = annotation.value
    require(value == "100 20000 2000000 2000000000000 3.14 3.14 true \u03c0 :)", { "Annotation value: $value" })
}