// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
enum class Some {
    FIRST,
    SECOND;
}

typealias Other = Some

fun foo(o: Other) = <!WHEN_ON_SEALED!>when (o) {
    FIRST -> 1
    SECOND -> 2
}<!>

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, integerLiteral, smartcast,
typeAliasDeclaration, whenExpression, whenWithSubject */
