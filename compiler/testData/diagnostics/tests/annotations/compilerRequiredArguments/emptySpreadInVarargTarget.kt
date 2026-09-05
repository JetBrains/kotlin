// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP

@Target(*[], AnnotationTarget.FIELD)
annotation class EmptySpreadThenField

@Target(AnnotationTarget.FIELD, *[])
annotation class FieldThenEmptySpread

@Target(*arrayOf(), AnnotationTarget.FIELD)
annotation class EmptyArrayOfThenField

@Target(*[AnnotationTarget.FIELD], AnnotationTarget.CLASS)
annotation class SpreadAndPlain

@Target(*[])
annotation class OnlyEmptySpread

class C {
    @EmptySpreadThenField
    val a = 1

    @FieldThenEmptySpread
    val b = 1

    @EmptyArrayOfThenField
    val c = 1

    @SpreadAndPlain
    val d = 1

    <!WRONG_ANNOTATION_TARGET!>@OnlyEmptySpread<!>
    val e = 1
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, collectionLiteral, integerLiteral, propertyDeclaration */
