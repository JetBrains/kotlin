// Annotations used for annotations :)
enum class Target {
    CLASSIFIER,
    FUNCTION
}

target(Target.CLASSIFIER)
public annotation class target(vararg val allowedTargets: Target)

target(Target.CLASSIFIER)
public annotation(AnnotationRetention.SOURCE) class annotation(
    val retention: AnnotationRetention = AnnotationRetention.RUNTIME,
    val repeatable: Boolean = false
)

annotation class some

