// Annotations used for annotations :)
enum class Target {
    CLASSIFIER,
    FUNCTION
}

enum class Retention {
    SOURCE,
    BINARY,
    RUNTIME
}

target(Target.CLASSIFIER) 
public annotation class target(vararg val allowedTargets: Target)

target(Target.CLASSIFIER)
public annotation(Retention.SOURCE) class annotation(
    val retention: Retention = Retention.RUNTIME,
    val repeatable: Boolean = false
)
