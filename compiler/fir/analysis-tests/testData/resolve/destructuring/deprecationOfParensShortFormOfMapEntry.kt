// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +NameBasedDestructuring, +DeprecateNameMismatchInShortDestructuringWithParentheses, -EnableNameBasedDestructuringShortForm
// RENDER_DIAGNOSTICS_FULL_TEXT
fun declaration(x: Map.Entry<String, Int>) {
    // deprecations
    if (true) { val (<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>value<!>, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>key<!>) = x }
    if (true) { val (key, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>bb<!>) = x }
    if (true) { val (<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>, value) = x }
    if (true) { val (<!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>bb<!>) = x }
    if (true) { val (<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>, <!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>) = x }
    if (true) { val (<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>) = x }

    // ok
    if (true) { val (key, value) = x }

    if (true) { (val value, val key) = x }
    if (true) { (val bb = value, val aa = key) = x }
    if (true) { (val aa = key, val bb = value) = x }

    if (true) { val [aa, bb] = x }
    if (true) { [val aa, val bb] = x }
}

fun loop(x: List<Map.Entry<String, Int>>) {
    // deprecations
    for ((<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>value<!>, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>key<!>) in x) {}
    for ((key, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>bb<!>) in x) {}
    for ((<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>, value) in x) {}
    for ((<!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>bb<!>) in x) {}
    for ((<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>, <!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>) in x) {}
    for ((<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>) in x) {}

    // ok
    for ((key, value) in x) {}
    for ((val value, val key) in x) {}
    for ((val bb = value, val aa = key) in x) {}
    for ((val aa = key, val bb = value) in x) {}
    for ([aa, bb] in x) {}
    for ([val aa, val bb] in x) {}
}

fun lambda() {
    fun foo(f: (Map.Entry<String, Int>) -> Unit) {}

    // deprecations
    foo { (<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>value<!>, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>key<!>) -> }
    foo { (key, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>bb<!>) -> }
    foo { (<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>, value) -> }
    foo { (<!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>bb<!>) -> }
    foo { (<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>, <!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>) -> }
    foo { (<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>) -> }

    // ok
    foo { (key, value) -> }
    foo { (val value, val key) -> }
    foo { (val bb = value, val aa = key) -> }
    foo { (val aa = key, val bb = value) -> }
    foo { [aa, bb] -> }
    foo { [val aa, val bb] -> }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, destructuringDeclaration, forLoop, functionDeclaration, functionalType,
ifExpression, lambdaLiteral, localFunction, localProperty, primaryConstructor, propertyDeclaration, unnamedLocalVariable */
