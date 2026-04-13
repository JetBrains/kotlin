// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-60299

private typealias T = Unit

internal fun x(): T {
    val something = "OK"
    something.hashCode()
}

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, propertyDeclaration, stringLiteral, typeAliasDeclaration */
