// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

@Deprecated("")
class Old

class IncompleteFunctionType {
    val incompleteFunctionType: context(Int.()<!SYNTAX!><!>
<!SYNTAX!><!>}

class UnresolvedReceiver {
    val unresolvedReceiver: context(<!UNRESOLVED_REFERENCE!>Unresolved<!>.()<!SYNTAX!><!>
<!SYNTAX!><!>}

class UnresolvedEntry {
    val unresolved: context(<!UNRESOLVED_REFERENCE!>Unresolved<!><!SYNTAX!><!>
}

class NamedDeprecated {
    val named: context(a: Int, b: <!DEPRECATION!>Old<!><!SYNTAX!><!>
}

fun valueParameter(p: context(<!UNRESOLVED_REFERENCE!>Unresolved<!><!SYNTAX!><!> {}

fun closedInUnclosedParameterList(p: context(<!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!> {}

fun after() {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, propertyDeclaration, stringLiteral */
