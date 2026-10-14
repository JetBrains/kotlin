@Target(allowedTargets = [AnnotationTarget.PROPERTY, AnnotationTarget.FIELD])
public annotation class AnnotationWithArrayLiteral

@Target(allowedTargets = [AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, 1])
public annotation class AnnotationWithArrayLiteralAndIncorrectValue

@Target(*[AnnotationTarget.PROPERTY, AnnotationTarget.FIELD])
public annotation class AnnotationWithArrayLiteralAndSpreadOperator

@Target(allowedTargets = arrayOf(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD))
public annotation class AnnotationWithArrayOf

@Target(*arrayOf(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD))
public annotation class AnnotationWithArrayOfAndSpreadOperator

@Target(arrayOf(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD))
public annotation class AnnotationWithArrayOfWithoutSpreadOperator

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
public annotation class AnnotationWithVararg