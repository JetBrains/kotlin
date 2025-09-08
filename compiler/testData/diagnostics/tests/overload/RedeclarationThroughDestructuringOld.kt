// LANGUAGE: -NameBasedDestructuring -DeprecateNameMismatchInShortDestructuringWithParentheses -EnableNameBasedDestructuringShortForm
// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

data class Example(val a: String, val b: Int) {
    fun testRedeclaration(e: Example){
        val (<!REDECLARATION!>b<!>, <!NAME_SHADOWING, REDECLARATION!>b<!>) = e
    }

}

/* GENERATED_FIR_TAGS: classDeclaration, data, destructuringDeclaration, functionDeclaration, localProperty,
primaryConstructor, propertyDeclaration */
