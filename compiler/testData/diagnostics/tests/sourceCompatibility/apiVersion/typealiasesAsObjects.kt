// RUN_PIPELINE_TILL: FRONTEND
// API_VERSION: 1.0

@SinceKotlin("1.1")
object Since_1_1 {
    val x = 42
}

typealias Since_1_1_Alias = <!UNRESOLVED_REFERENCE!>Since_1_1<!>

val test1 = <!NO_COMPANION_OBJECT!>Since_1_1_Alias<!>
val test2 = Since_1_1_Alias.<!UNRESOLVED_REFERENCE!>x<!>

/* GENERATED_FIR_TAGS: integerLiteral, objectDeclaration, propertyDeclaration, stringLiteral, typeAliasDeclaration */
