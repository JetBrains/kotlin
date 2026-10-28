// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

@Deprecated("")
class Old

interface Properties {
    val ctx1: context(Int)<!SYNTAX!><!>
    val ctx2: context(Int) (<!SYNTAX!><!>)<!SYNTAX!><!>
    val ctx3: context(Int) (<!SYNTAX!><!>) <!SYNTAX!>-<!>
    val ctx4: context(Int) Int<!SYNTAX!><!>
    val ctx4Nullable: context(Int) String?<!SYNTAX!><!>
    val ctx5: context(Int) Int.<!SYNTAX!><!>
    val ctx5Parenthesized: context(Int) (String).<!SYNTAX!><!>
    val named: context(a: Int)<!SYNTAX!><!>
    val severalEntries: context(Int, String)<!SYNTAX!><!>
    val annotated: context(@Anno("1" + "2") Int)<!SYNTAX!><!>
    val typeArgument: List<context(Int)<!SYNTAX!><!>>

    val unresolved: context(Unresolved)<!SYNTAX!><!>
    val deprecated: context(Old)<!SYNTAX!><!>
    val unresolvedReceiver: context(Int) <!UNRESOLVED_REFERENCE!>Unresolved<!>.<!SYNTAX!><!>
    val unresolvedReturn: context(Int) <!UNRESOLVED_REFERENCE!>Unresolved<!><!SYNTAX!><!>
    val deprecatedReturn: context(Int) Old<!SYNTAX!><!>
}

fun returnType(): context(Int)<!SYNTAX!><!> = TODO()

fun valueParameter(p: context(Int) String.<!SYNTAX!><!>) {}

fun <T> intersection(p: context(Int) T & Any<!SYNTAX!><!>) {}

fun body() {
    val local: context(Int)<!SYNTAX!><!> = TODO()
    val localReceiver: context(Int) Int. <!SYNTAX!>=<!> <!SYNTAX!>TODO<!><!SYNTAX!>()<!>
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, dnnType, functionDeclaration, interfaceDeclaration,
localProperty, nullableType, primaryConstructor, propertyDeclaration, stringLiteral, typeParameter */
