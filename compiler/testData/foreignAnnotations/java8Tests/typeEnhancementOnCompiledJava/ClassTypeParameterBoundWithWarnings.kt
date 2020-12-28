// !LANGUAGE: +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// SKIP_TXT

// TODO: report warnings "UPPER_BOUND_VIOLATED"
fun main(x: ClassTypeParameterBoundWithWarnings<String?>, y: ClassTypeParameterBoundWithWarnings<String>, a: String?, b: String) {
    val x2 = ClassTypeParameterBoundWithWarnings<String?>()
    val y2 = ClassTypeParameterBoundWithWarnings<String>()

    val x3 = ClassTypeParameterBoundWithWarnings(a)
    val y3 = ClassTypeParameterBoundWithWarnings(b)

    val x4: ClassTypeParameterBoundWithWarnings<String?> = ClassTypeParameterBoundWithWarnings()
    val y4: ClassTypeParameterBoundWithWarnings<String> = ClassTypeParameterBoundWithWarnings()
}
