// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CollectionLiteralsBasedAnnotationResolution
// LANGUAGE_FEATURE_TOGGLED: AllowAnnotationsOnArgumentsOfAnnotations

@Deprecated("", level = DeprecationLevel.WARNING)
const val A_LOT = 1_000_000

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION)
@Repeatable
annotation class VarargAnno(vararg val vs: Int = [@Suppress("DEPRECATION") A_LOT, 0])

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION)
@Repeatable
annotation class SimpleAnno(val v: Int = @Suppress("DEPRECATION") A_LOT)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION)
@Repeatable
annotation class ArrayAnno(val vs: IntArray = @Suppress("DEPRECATION") [A_LOT, A_LOT, A_LOT])

val noop = Unit

@VarargAnno(1, 2, @Suppress("DEPRECATION") A_LOT)
@VarargAnno(* @Suppress("DEPRECATION") [A_LOT, A_LOT, A_LOT])
@VarargAnno(*[@Suppress("DEPRECATION") A_LOT, @Suppress("DEPRECATION") A_LOT, @Suppress("DEPRECATION") A_LOT])
@SimpleAnno(<!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Suppress("DEPRECATION")<!> A_LOT)
@SimpleAnno(v = <!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Suppress("DEPRECATION")<!> A_LOT)
@SimpleAnno(v = (@Suppress("DEPRECATION") A_LOT) + (@Suppress("DEPRECATION") A_LOT))
@SimpleAnno(v = (@Suppress("DEPRECATION") A_LOT).toLong().toInt())
@SimpleAnno(v = <!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Suppress("DEPRECATION")<!> A_LOT.toLong().toInt())
@ArrayAnno([<!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Suppress("DEPRECATION")<!> A_LOT, 0, <!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Suppress("DEPRECATION")<!> A_LOT])
@ArrayAnno(<!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Suppress("DEPRECATION")<!> [A_LOT, A_LOT + A_LOT, A_LOT])
fun suppression() {
    // fails with type mismatch without CollectionLiteralsBasedAnnotationResolution
    @ArrayAnno(intArrayOf(0, * <!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Suppress("DEPRECATION")<!> [A_LOT], <!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Suppress("DEPRECATION")<!> A_LOT))
    noop
}

@VarargAnno(@Suppress("DEPRECATION") A_LOT, <!DEPRECATION!>A_LOT<!>)
@VarargAnno(<!DEPRECATION!>A_LOT<!> + @Suppress("DEPRECATION") A_LOT)
fun missedSuppression() {
    // fails with type mismatch without CollectionLiteralsBasedAnnotationResolution
    @ArrayAnno(intArrayOf(*[<!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Suppress("DEPRECATION")<!> A_LOT, <!DEPRECATION!>A_LOT<!>]))
    noop
}

/* GENERATED_FIR_TAGS: additiveExpression, annotationDeclaration, collectionLiteral, const, functionDeclaration,
integerLiteral, primaryConstructor, propertyDeclaration, stringLiteral, vararg */
