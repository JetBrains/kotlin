// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: ExplicitInlineValueClassesSyntax
// WITH_STDLIB

<!JVM_INLINE_WITHOUT_VALUE_CLASS!>@JvmInline<!>
<!INLINE_CLASS_DEPRECATED!>inline<!> class Old(val x: Int)

@JvmInline
<!INCOMPATIBLE_MODIFIERS, INLINE_CLASS_DEPRECATED!>inline<!> <!INCOMPATIBLE_MODIFIERS!>value<!> class New(val x: Int)

@JvmInline
<!INCOMPATIBLE_MODIFIERS, INLINE_CLASS_DEPRECATED!>value<!> <!INCOMPATIBLE_MODIFIERS!>inline<!> class NewReversed(val x: Int)

@JvmInline
<!INCOMPATIBLE_MODIFIERS, INLINE_CLASS_DEPRECATED!>inline<!> <!INCOMPATIBLE_MODIFIERS!>value<!> class TwoFields<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Int, val y: String)<!>

fun f(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> xs: New) {}
fun g(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> xs: NewReversed) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration, value, vararg */
