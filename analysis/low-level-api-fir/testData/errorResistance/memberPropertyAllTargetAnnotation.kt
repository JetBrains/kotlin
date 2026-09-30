// IGNORE_ERROR_RESISTANCE: KT-85410
import broken.lib.BrokenAnnotation

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
annotation class FieldAndGetterAnn

class Foo {
    @all:FieldAndGetterAnn @BrokenAnnotation
    val x: Int = 1
}