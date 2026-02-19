// RUN_PIPELINE_TILL: FRONTEND
// Class constructor parameter CAN be recursively annotated
class RecursivelyAnnotated(<!NOT_AN_ANNOTATION_CLASS!>@RecursivelyAnnotated(1)<!> val x: Int)

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, primaryConstructor, propertyDeclaration */
