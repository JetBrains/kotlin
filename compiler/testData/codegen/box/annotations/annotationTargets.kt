// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK

@Target(
    AnnotationTarget.FUNCTION, AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER
)
public annotation class TestAnn

fun box(): String {
    val testAnnClass = TestAnn::class.java
    val targetAnn = testAnnClass.getAnnotation(java.lang.annotation.Target::class.java)
    val targets = targetAnn.value
    if (targets.size != 2) {
        return targets.toList().toString()
    }
    if (targets.toSet() != setOf(java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.METHOD)) {
        return targets.toList().toString()
    }
    return "OK"
}
