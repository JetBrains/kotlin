package myPack

@Target(AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.EXPRESSION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class Anno(val number: Int)

@Anno(localAnnotationProperty)
const val localAnnotationProperty = 42

@Anno(expressionAnnotationProperty)
const val expressionAnnotationProperty = 42

@Anno(setterAnnotationProperty)
const val setterAnnotationProperty = 42

@Anno(receiverAnnotationProperty)
const val receiverAnnotationProperty = 42

fun topLevelFunction() = run {
    @Anno(localAnnotationProperty)
    var @receiver:Anno(receiverAnnotationProperty) Int.variableToResolve = @Anno(expressionAnnotationProperty) "str"
}
