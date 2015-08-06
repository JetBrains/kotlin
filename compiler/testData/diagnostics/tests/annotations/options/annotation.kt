// Annotations used for annotations :)
enum class Target {
    CLASSIFIER,
    FUNCTION
}

<!NOT_AN_ANNOTATION_CLASS!>target(Target.CLASSIFIER)<!>
public <!NOT_AN_ANNOTATION_CLASS!>annotation<!> class target(vararg val allowedTargets: Target)

<!NOT_AN_ANNOTATION_CLASS!>target(Target.CLASSIFIER)<!>
public <!NOT_AN_ANNOTATION_CLASS!>annotation(AnnotationRetention.SOURCE)<!> class annotation(
    val retention: AnnotationRetention = AnnotationRetention.RUNTIME,
    val repeatable: Boolean = false
)

<!NOT_AN_ANNOTATION_CLASS!>annotation<!> class some