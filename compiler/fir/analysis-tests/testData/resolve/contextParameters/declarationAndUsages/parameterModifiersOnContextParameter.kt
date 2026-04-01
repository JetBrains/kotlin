// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

context(<!WRONG_MODIFIER_TARGET!>vararg<!> a: String)
fun test1() {}

context(noinline a: ()->String)
<!NOTHING_TO_INLINE!>inline<!> fun test2() {}

context(<!CONTEXT_PARAMETER_MUST_BE_NOINLINE!>crossinline a: ()->String<!>)
inline fun test3() {}

context(<!WRONG_MODIFIER_TARGET!>vararg<!> a: String)
val property1: String
    get() = ""

context(noinline a: ()->String)
val property2: String
    inline get() = ""

context(<!CONTEXT_PARAMETER_MUST_BE_NOINLINE!>crossinline a: ()->String<!>)
val property3: String
    inline get() = ""

/* GENERATED_FIR_TAGS: crossinline, functionDeclaration, functionDeclarationWithContext, functionalType, getter, inline,
noinline, propertyDeclaration, propertyDeclarationWithContext, stringLiteral, vararg */
