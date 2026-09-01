// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// DIAGNOSTICS: -UNUSED_PARAMETER

open class Base {
    fun `foo$default`(i: Int, mask: Int, mh: Any) {}
}

object Derived : Base() {
    <!ACCIDENTAL_OVERRIDE!>@JvmStatic fun foo(i: Int = 0)<!> {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, objectDeclaration */
