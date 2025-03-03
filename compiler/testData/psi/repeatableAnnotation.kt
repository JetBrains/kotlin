class RepeatableAnnotation {
    @MyRepeatableAnnotation(1)
    class ClassWithOneRepeatableAnnotation

    @MyRepeatableAnnotation(1)
    @MyRepeatableAnnotation(2)
    class ClassWithTwoRepeatableAnnotations

    @MyRepeatableAnnotation(1)
    @MyRepeatableAnnotation(2)
    @MyRepeatableAnnotation(3)
    class ClassWithThreeRepeatableAnnotations
}

@Repeatable
annotation class MyRepeatableAnnotation(val index: Int)