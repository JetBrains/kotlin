// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87072

const val VERSION = "99.9"

@SinceKotlin(<!NEWER_VERSION_IN_SINCE_KOTLIN!>VERSION<!>)
fun sinceConst() {}

@SinceKotlin(<!NEWER_VERSION_IN_SINCE_KOTLIN!>"99.9"<!>)
fun sinceLiteral() {}

fun use() {
    sinceConst()
    <!UNRESOLVED_REFERENCE!>sinceLiteral<!>()
}

/* GENERATED_FIR_TAGS: const, functionDeclaration, propertyDeclaration, stringLiteral */
