// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_FIR_DIAGNOSTICS
// IGNORE_PHASE_VERIFICATION: invalid code inside annotations
// MODULE: m1-common
// FILE: common.kt

annotation class C(val f: String)

const val flag = true

@C(
    f = <!ANNOTATION_ARGUMENT_MUST_BE_CONST, ANNOTATION_ARGUMENT_MUST_BE_CONST{JVM}!>when (flag) {
        true -> "OK"
        false -> "Not OK"
    }<!>
)
object O

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

fun bar() = O

/* GENERATED_FIR_TAGS: annotationDeclaration, const, equalityExpression, functionDeclaration, objectDeclaration,
primaryConstructor, propertyDeclaration, smartcast, stringLiteral, whenExpression, whenWithSubject */
