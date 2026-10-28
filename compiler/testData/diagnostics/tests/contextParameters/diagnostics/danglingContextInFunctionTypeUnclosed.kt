// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

@Deprecated("")
class Old

class IncompleteFunctionType {
    val incompleteFunctionType: context(Int.()<!SYNTAX!><!>
<!SYNTAX!><!>}

class UnresolvedReceiver {
    val unresolvedReceiver: context(Unresolved.()<!SYNTAX!><!>
<!SYNTAX!><!>}

class UnresolvedEntry {
    val unresolved: context(Unresolved<!SYNTAX!><!>
}

class NamedDeprecated {
    val named: context(a: Int, b: Old<!SYNTAX!><!>
}

fun valueParameter(p: context(Unresolved<!SYNTAX!><!> {}

fun closedInUnclosedParameterList(p: context(Unresolved)<!SYNTAX!><!> {}

fun after() {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, propertyDeclaration, stringLiteral */
