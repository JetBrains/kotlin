// !LANGUAGE: +RepeatableAnnotations
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8

@Repeatable
annotation class RetentionRuntime

@Repeatable
@Retention(AnnotationRetention.BINARY)
annotation class RetentionBinary

@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class RetentionSource

@Repeatable
@Target(AnnotationTarget.CLASS)
annotation class TargetClassOnly

@Repeatable
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.TYPE)
annotation class TargetAnnotationClassAndTypeOnly

@Repeatable
@Target()
annotation class TargetEmpty
