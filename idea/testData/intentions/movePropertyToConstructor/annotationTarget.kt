annotation class Ann(val i: Int, val j: Int)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class PropertyFieldAnn(val i: Int, val j: Int)

@Target(AnnotationTarget.PROPERTY)
annotation class PropertyAnn(val i: Int, val j: Int)

@Target(AnnotationTarget.FIELD)
annotation class FieldAnn(val i: Int, val j: Int)

class Test {
    @Ann(1, 2)
    @PropertyFieldAnn(3, 4)
    @PropertyAnn(5, 6)
    @FieldAnn(7, 8)
    @get:Ann(9, 0)
    val <caret>foo = ""
}