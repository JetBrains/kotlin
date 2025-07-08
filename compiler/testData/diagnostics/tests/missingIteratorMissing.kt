// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-58284

fun foo() {
    for (i in <!ITERATOR_MISSING!>0<!>) {}
}

fun String.iterator(): Iterator<Int> = TODO()

/* GENERATED_FIR_TAGS: forLoop, funWithExtensionReceiver, functionDeclaration, integerLiteral, localProperty,
propertyDeclaration */
