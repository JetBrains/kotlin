// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring, +DeprecateNameMismatchInShortDestructuringWithParentheses, -EnableNameBasedDestructuringShortForm
// RENDER_DIAGNOSTICS_FULL_TEXT
data class User(val name: String, val age: Int) {
    operator fun component3(): String = TODO("irrelevant")
}

fun usage(user: User) {
    val (name, age, <!DESTRUCTURING_SHORT_FORM_OF_NON_DATA_CLASS!>otherName<!>) = user
}

/* GENERATED_FIR_TAGS: classDeclaration, data, destructuringDeclaration, functionDeclaration, localProperty, operator,
primaryConstructor, propertyDeclaration, stringLiteral */
