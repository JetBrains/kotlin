// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

<!UNSUPPORTED!>context(s: String)<!>
class C {
    <!UNSUPPORTED!>context(s: String)<!>
    constructor() {}

    context(s: String)
    fun f(){}

    context(_: String)
    val p: String get() = ""
}

context(s: String)
fun f(){}

context(_: String)
val p: String get() = ""

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, getter,
propertyDeclaration, propertyDeclarationWithContext, secondaryConstructor, stringLiteral */
