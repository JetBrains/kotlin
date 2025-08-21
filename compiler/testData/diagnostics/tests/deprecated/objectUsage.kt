// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
@Deprecated("Object")
object Obsolete {
    fun use() {}
}

fun useObject() {
    <!DEPRECATION!>Obsolete<!>.use()
    val x = <!DEPRECATION!>Obsolete<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, objectDeclaration, propertyDeclaration, stringLiteral */
