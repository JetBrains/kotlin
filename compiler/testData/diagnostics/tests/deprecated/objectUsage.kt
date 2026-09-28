// RUN_PIPELINE_TILL: CODEGEN
@Deprecated("Object")
object Obsolete {
    fun use() {}
}

fun useObject() {
    <!DEPRECATION!>Obsolete<!>.use()
    val x = <!DEPRECATION!>Obsolete<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, objectDeclaration, propertyDeclaration, stringLiteral */
