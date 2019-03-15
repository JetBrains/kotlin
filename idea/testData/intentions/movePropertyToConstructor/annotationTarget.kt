annotation class Ann1(val i: Int, val j: Int)

@Target(AnnotationTarget.PROPERTY)
annotation class Ann2(val i: Int, val j: Int)

@Target(AnnotationTarget.FIELD)
annotation class Ann3(val i: Int, val j: Int)

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class Ann4(val i: Int, val j: Int)

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class Ann5(val i: Int, val j: Int)

class Test {
    @get:Ann1(0, 0)
    @Ann1(1, 11)
    @Ann2(2, 22)
    @Ann3(3, 33)
    @Ann4(4, 44)
    @Ann5(5, 55)
    val <caret>foo = ""
}