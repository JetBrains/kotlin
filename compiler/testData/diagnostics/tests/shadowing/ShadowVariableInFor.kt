// FIR_IDENTICAL
// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun ff(): Int {
    var i = 1
    for (i in 0..10) {
    }

    for ([i, j] in listOf(Pair(1, 2))) {
        val i = i
    }

    return i
}

/* GENERATED_FIR_TAGS: forLoop, functionDeclaration, integerLiteral, localProperty, propertyDeclaration, rangeExpression */
