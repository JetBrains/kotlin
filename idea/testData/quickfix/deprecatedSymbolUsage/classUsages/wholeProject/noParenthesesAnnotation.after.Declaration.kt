package dependency

@Deprecated("", ReplaceWith("dependency.NewAnnotation"))
annotation class OldAnnotation

annotation class NewAnnotation()

@NewAnnotation
class C