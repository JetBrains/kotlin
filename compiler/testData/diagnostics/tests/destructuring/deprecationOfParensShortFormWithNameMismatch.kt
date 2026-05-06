// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring, +DeprecateNameMismatchInShortDestructuringWithParentheses, -EnableNameBasedDestructuringShortForm
// RENDER_DIAGNOSTICS_FULL_TEXT
data class Tuple(val a: String, val b: Int)

fun declaration(x: Tuple) {
    // deprecations
    if (true) { val (<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>b<!>, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>a<!>) = x }
    if (true) { val (a, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>bb<!>) = x }
    if (true) { val (<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>, b) = x }
    if (true) { val (<!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>bb<!>) = x }
    if (true) { val (<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>, <!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>) = x }
    if (true) { val (<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>) = x }

    // ok
    if (true) { val (a, b) = x }

    if (true) { (val b, val a) = x }
    if (true) { (val bb = b, val aa = a) = x }
    if (true) { (val aa = a, val bb = b) = x }

    if (true) { val [aa, bb] = x }
    if (true) { [val aa, val bb] = x }
}

fun loop(x: List<Tuple>) {
    // deprecations
    for ((<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>b<!>, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>a<!>) in x) {}
    for ((a, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>bb<!>) in x) {}
    for ((<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>, b) in x) {}
    for ((<!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>bb<!>) in x) {}
    for ((<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>, <!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>) in x) {}
    for ((<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>) in x) {}

    // ok
    for ((a, b) in x) {}
    for ((val b, val a) in x) {}
    for ((val bb = b, val aa = a) in x) {}
    for ((val aa = a, val bb = b) in x) {}
    for ([aa, bb] in x) {}
    for ([val aa, val bb] in x) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}

    // deprecations
    foo { (<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>b<!>, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>a<!>) -> }
    foo { (a, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>bb<!>) -> }
    foo { (<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>, b) -> }
    foo { (<!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>, <!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>bb<!>) -> }
    foo { (<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>, <!DESTRUCTURING_SHORT_FORM_UNDERSCORE!>_<!>) -> }
    foo { (<!DESTRUCTURING_SHORT_FORM_NAME_MISMATCH!>aa<!>) -> }

    // ok
    foo { (a, b) -> }
    foo { (val b, val a) -> }
    foo { (val bb = b, val aa = a) -> }
    foo { (val aa = a, val bb = b) -> }
    foo { [aa, bb] -> }
    foo { [val aa, val bb] -> }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, destructuringDeclaration, forLoop, functionDeclaration, functionalType,
ifExpression, lambdaLiteral, localFunction, localProperty, primaryConstructor, propertyDeclaration, unnamedLocalVariable */
