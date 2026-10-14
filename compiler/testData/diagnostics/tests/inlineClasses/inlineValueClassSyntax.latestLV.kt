// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// COMPARE_WITH_LIGHT_TREE
// LANGUAGE_FEATURE_TOGGLED: ExplicitInlineValueClassesSyntax

inline value class A(val x: Int)
value inline class B(val x: String)

inline value class C(val x: Int, val y: String)
abstract inline value class D
<!WRONG_MODIFIER_TARGET!>inline<!> value object E

fun f(vararg xs: A) {}
fun g(vararg xs: B) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration, value, vararg */
