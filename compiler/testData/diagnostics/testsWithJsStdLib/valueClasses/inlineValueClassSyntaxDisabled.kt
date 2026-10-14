// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED
//              ^^^ PSI and LT report INLINE_CLASS_DEPRECATED on different modifiers
// LANGUAGE: -ExplicitInlineValueClassesSyntax

<!INCOMPATIBLE_MODIFIERS!>inline<!> <!INCOMPATIBLE_MODIFIERS!>value<!> class A(val x: Int)
<!INCOMPATIBLE_MODIFIERS!>value<!> <!INCOMPATIBLE_MODIFIERS!>inline<!> class B(val x: String)

fun f(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> xs: A) {}
fun g(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> xs: B) {}
