// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

open class C {
    fun <T> some(s: String): T = null!!

    context(s: String)
    fun <T> some2(text: String): T = null!!

    context(s: String)
    fun <T> some3(text: String): T = null!!
}

open class X5 : C() {
    context(s: String)
    fun some(text: String): String = ""

    context(s: String)
    <!CONFLICTING_OVERLOADS!>fun some2(text: String): String<!> = ""

    context(s: Int)
    fun some3(text: String): String = ""
}
