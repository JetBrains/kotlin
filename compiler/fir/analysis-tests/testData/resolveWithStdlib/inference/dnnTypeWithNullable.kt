// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75924

fun <T, R> process(input: Array<T>) {
    input.mapIndexed { i, it -> "item$i" to (listOf(it) <!UNCHECKED_CAST!>as? R<!> ?: throw IllegalStateException()) }
        .map { [a, b] -> val p: String = <!INITIALIZER_TYPE_MISMATCH!>b<!> }
}

/* GENERATED_FIR_TAGS: dnnType, elvisExpression, functionDeclaration, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral, typeParameter */
