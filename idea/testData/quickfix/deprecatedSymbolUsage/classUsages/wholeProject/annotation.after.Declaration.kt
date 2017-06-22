package dependency

@Deprecated("", ReplaceWith("dependency.NewAnnotation"))
annotation class OldAnnotation(val p: Int = 0)

annotation class NewAnnotation(val p: Int = 0, val newP: String = "")

@NewAnnotation
class C