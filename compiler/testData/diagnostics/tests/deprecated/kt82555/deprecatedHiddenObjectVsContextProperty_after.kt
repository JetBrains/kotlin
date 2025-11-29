// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82555
// LANGUAGE: +ContextParameters, +SkipHiddenObjectsInResolution
// FIR_IDENTICAL
//  ^ K1 is ignored

object Obj {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    object <!REDECLARATION!>Nested<!>

    context(str: String)
    val <!REDECLARATION!>Nested<!> get() = 42
}

@Deprecated("", level = DeprecationLevel.HIDDEN)
object <!REDECLARATION!>TopLevel<!>

context(str: String)
val <!REDECLARATION!>TopLevel<!> get() = 42

fun test() {
    with("") {
        Obj.Nested
        TopLevel
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, getter, integerLiteral, lambdaLiteral, nestedClass, objectDeclaration,
propertyDeclaration, propertyDeclarationWithContext, stringLiteral */
