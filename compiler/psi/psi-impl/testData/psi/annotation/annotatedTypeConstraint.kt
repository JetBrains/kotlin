// FILE: I1.kt
interface I1

// FILE: I2.kt
interface I2

// FILE: Anno1.kt
@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno1

// FILE: Anno2.kt
@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno2

// FILE: foo.kt
@Suppress("ANNOTATION_IN_WHERE_CLAUSE_ERROR")
fun <@Anno1 T : I1> foo() where @Anno2 T : I2 {

}
