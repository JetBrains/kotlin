// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +NameBasedDestructuring, +DeprecateNameMismatchInShortDestructuringWithParentheses, -EnableNameBasedDestructuringShortForm
// RENDER_DIAGNOSTICS_FULL_TEXT
fun declaration(x: List<String>) {
    // deprecations
    if (true) { val (<!DESTRUCTURING_SHORT_FORM_OF_NON_DATA_CLASS!>b<!>, a) = x }
    if (true) { val (<!DESTRUCTURING_SHORT_FORM_OF_NON_DATA_CLASS!>a<!>, bb) = x }
    if (true) { val (<!DESTRUCTURING_SHORT_FORM_OF_NON_DATA_CLASS!>aa<!>, b) = x }
    if (true) { val (<!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>, bb) = x }
    if (true) { val (<!DESTRUCTURING_SHORT_FORM_OF_NON_DATA_CLASS!>aa<!>, <!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>) = x }
    if (true) { val (<!DESTRUCTURING_SHORT_FORM_OF_NON_DATA_CLASS!>aa<!>) = x }

    // ok
    if (true) { val [aa, bb] = x }
    if (true) { [val aa, val bb] = x }
}

fun loop(x: List<List<String>>) {
    // deprecations
    for ((<!DESTRUCTURING_SHORT_FORM_OF_NON_DATA_CLASS!>b<!>, a) in x) {}
    for ((<!DESTRUCTURING_SHORT_FORM_OF_NON_DATA_CLASS!>a<!>, bb) in x) {}
    for ((<!DESTRUCTURING_SHORT_FORM_OF_NON_DATA_CLASS!>aa<!>, b) in x) {}
    for ((<!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>, bb) in x) {}
    for ((<!DESTRUCTURING_SHORT_FORM_OF_NON_DATA_CLASS!>aa<!>, <!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>) in x) {}
    for ((<!DESTRUCTURING_SHORT_FORM_OF_NON_DATA_CLASS!>aa<!>) in x) {}

    // ok
    for ([aa, bb] in x) {}
    for ([val aa, val bb] in x) {}
}

fun lambda() {
    fun foo(f: (List<String>) -> Unit) {}

    // deprecations
    foo { (<!DESTRUCTURING_SHORT_FORM_OF_NON_DATA_CLASS!>b<!>, a) -> }
    foo { (<!DESTRUCTURING_SHORT_FORM_OF_NON_DATA_CLASS!>a<!>, bb) -> }
    foo { (<!DESTRUCTURING_SHORT_FORM_OF_NON_DATA_CLASS!>aa<!>, b) -> }
    foo { (<!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>, bb) -> }
    foo { (<!DESTRUCTURING_SHORT_FORM_OF_NON_DATA_CLASS!>aa<!>, <!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>) -> }
    foo { (<!DESTRUCTURING_SHORT_FORM_OF_NON_DATA_CLASS!>aa<!>) -> }

    // ok
    foo { [aa, bb] -> }
    foo { [val aa, val bb] -> }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, destructuringDeclaration, forLoop, functionDeclaration, functionalType,
ifExpression, lambdaLiteral, localFunction, localProperty, primaryConstructor, propertyDeclaration, unnamedLocalVariable */
