// CHECK_BY_JAVA_FILE
// JVM_TARGET: 1.6

@Target()
annotation class A0
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class A1
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class A2
