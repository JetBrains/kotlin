// FIR_COMPARISON
@Target(AnnotationTarget.TYPE)
annotation class AnnType

class AnnFalse

val v: @Ann<caret> Int = 1

// EXIST: AnnType
// ABSENT: AnnFalse