// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// COMPARE_WITH_LIGHT_TREE
// LANGUAGE_FEATURE_TOGGLED: ExplicitInlineValueClassesSyntax

inline value class A(val x: Int)
value inline class B(val x: String)

inline value class C<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Int, val y: String)<!>
<!VALUE_CLASS_NOT_FINAL!>abstract<!> <!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>inline<!> value class D
<!WRONG_MODIFIER_TARGET!>inline<!> value object E

fun f(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> xs: A) {}
fun g(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> xs: B) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration, value, vararg */
