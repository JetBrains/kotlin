// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-86978

import kotlin.annotation.AnnotationTarget.FIELD

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Marker

@Target(@Marker @Unresolved AnnotationTarget.FIELD)
annotation class Annotated1

@Target(@Marker @Unresolved kotlin.annotation.AnnotationTarget.FIELD)
annotation class Annotated2

@Target((@Marker @Unresolved AnnotationTarget).FIELD)
annotation class Annotated3

@Target((@Marker @Unresolved kotlin.annotation.AnnotationTarget).FIELD)
annotation class Annotated4

@Target((kotlin.annotation).AnnotationTarget.FIELD)
annotation class Parenthesized

@Target((@Marker @Unresolved kotlin).annotation.AnnotationTarget.FIELD)
annotation class Annotated5

@Target(@Marker @Unresolved FIELD)
annotation class Annotated6

@Target(kotlin<Int>.annotation.AnnotationTarget.FIELD)
annotation class TypeArgumentOnFirstPart

@Target(kotlin.annotation<Int>.AnnotationTarget.FIELD)
annotation class TypeArgumentOnSecondPart

class C {
    @Annotated1
    @Annotated2
    @Annotated3
    @Annotated4
    @Parenthesized
    @Annotated5
    @Annotated6
    @TypeArgumentOnFirstPart
    @TypeArgumentOnSecondPart
    val field = 1
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, integerLiteral, propertyDeclaration */
