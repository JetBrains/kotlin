// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

context(<!WRONG_MODIFIER_TARGET!>vararg<!> a: String)
fun test1() {}

context(<!WRONG_MODIFIER_TARGET!>noinline<!> a: ()->String)
<!NOTHING_TO_INLINE!>inline<!> fun test2() {}

context(<!WRONG_MODIFIER_TARGET!>crossinline<!> a: ()->String)
<!NOTHING_TO_INLINE!>inline<!> fun test3() {}

context(<!WRONG_MODIFIER_CONTAINING_DECLARATION, WRONG_MODIFIER_TARGET!>vararg<!> a: String)
val property1: String
    get() = ""

context(<!WRONG_MODIFIER_TARGET!>noinline<!> a: ()->String)
val property2: String
    inline get() = ""

context(<!WRONG_MODIFIER_TARGET!>crossinline<!> a: ()->String)
val property3: String
    inline get() = ""
