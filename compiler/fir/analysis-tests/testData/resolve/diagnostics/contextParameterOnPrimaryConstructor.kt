// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

annotation class Anno(val value: String)

class Clazz
<!MISSING_CONSTRUCTOR_KEYWORD!>@Anno("str")
<!UNSUPPORTED!>context(c:Clazz)<!><!><!SYNTAX!><!>{}

class Another @Anno("str") <!UNSUPPORTED!>context(c:Another)<!> constructor(i: Int)

<!UNSUPPORTED!>context(_: Clazz)<!>
class OneMore @Anno("str") <!UNSUPPORTED!>context(c:<!UNRESOLVED_REFERENCE!>Unresolved<!>)<!> constructor(s: String)

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
