import broken.lib.BrokenAnnotation

@Target(AnnotationTarget.FIELD)
annotation class FieldTargetAnn

@FieldTargetAnn @BrokenAnnotation
val x = 1