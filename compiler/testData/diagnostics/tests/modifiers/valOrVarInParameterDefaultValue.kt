// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-91790

@Target(AnnotationTarget.FIELD)
annotation class FieldAnn

// `WRONG_ANNOTATION_TARGET` or `WRONG_MODIFIER_TARGET` should be reported on all lines below because
// a parameter without `val`/`var` is a plain value parameter, even if its default value declares variables or properties
// So annotations with the FIELD target and visibility modifiers are not applicable to it.

class LocalVal(@FieldAnn x: Int = run { val a = 1; a }, private y: Int = run { val a = 1; a })

class LocalVar(@FieldAnn x: Int = run { var a = 1; a }, private y: Int = run { var a = 1; a })

class ObjectProperty(@FieldAnn x: Int = object { val a = 1 }.a, private y: Int = object { val a = 1 }.a)

fun function(@FieldAnn x: Int = run { val a = 1; a }) {}

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousObjectExpression, classDeclaration, functionDeclaration,
integerLiteral, lambdaLiteral, localProperty, primaryConstructor, propertyDeclaration */
