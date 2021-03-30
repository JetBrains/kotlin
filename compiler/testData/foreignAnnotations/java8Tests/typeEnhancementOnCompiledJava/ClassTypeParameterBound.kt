// !LANGUAGE: +TypeEnhancementImprovementsInStrictMode +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// SKIP_TXT

fun main(x: ClassTypeParameterBound<<!UPPER_BOUND_VIOLATED!>String?<!>>, y: ClassTypeParameterBound<String>, a: String?, b: String) {
    val x2 = ClassTypeParameterBound<<!UPPER_BOUND_VIOLATED!>String?<!>>()
    val y2 = ClassTypeParameterBound<String>()

    val x3 = ClassTypeParameterBound(<!TYPE_MISMATCH!>a<!>)
    val y3 = ClassTypeParameterBound(b)

    val x4: ClassTypeParameterBound<<!UPPER_BOUND_VIOLATED!>String?<!>> = <!TYPE_MISMATCH!>ClassTypeParameterBound()<!>
    val y4: ClassTypeParameterBound<String> = ClassTypeParameterBound()
}
