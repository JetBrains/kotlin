// RUN_PIPELINE_TILL: FRONTEND

package one

fun test(f: NextMissing) {
    for(i in <!NEXT_OPERATOR_NONE_APPLICABLE!>f<!>) {}
}

interface Doo
operator fun Doo.next() {}

interface NextMissing {
    operator fun iterator(): NextMissing2
}

interface NextMissing2 {
    operator fun hasNext(): Boolean
}

/* GENERATED_FIR_TAGS: forLoop, funWithExtensionReceiver, functionDeclaration, interfaceDeclaration, localProperty,
operator, propertyDeclaration */
