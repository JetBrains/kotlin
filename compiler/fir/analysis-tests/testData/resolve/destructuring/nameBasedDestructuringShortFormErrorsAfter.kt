// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
class Tuple(val a: String, val b: Int)

fun declaration(x: Tuple) {
    if (true) { val (a, <!UNRESOLVED_REFERENCE!>second<!>,) = x }
    if (true) { var (<!UNRESOLVED_REFERENCE!>first<!>) = x }
    if (true) { val (<!UNRESOLVED_REFERENCE!>first<!>: String) = x }
    if (true) { <!INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION!>val (<!UNRESOLVED_REFERENCE!>first<!>)<!> }
    if (true) { val (a: <!INITIALIZER_TYPE_MISMATCH!>Int<!>, b: <!INITIALIZER_TYPE_MISMATCH!>String<!>) = x }
    if (true) { val (first: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> a, second: String <!INITIALIZER_TYPE_MISMATCH!>=<!> b) = x }
    if (true) { val (<!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>) = x }
    if (true) { val (<!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>: String) = x }
    // renaming
    if (true) { val (aa = <!UNRESOLVED_REFERENCE!>first<!>) = x }
    if (true) { val (aa: String = <!UNRESOLVED_REFERENCE!>first<!>) = x }
    if (true) { <!INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION!>val (a = <!UNRESOLVED_REFERENCE!>first<!>)<!> }
    if (true) { val (first: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> a, second: String <!INITIALIZER_TYPE_MISMATCH!>=<!> b) = x }
}

fun loop(x: List<Tuple>) {
    for ((<!UNRESOLVED_REFERENCE!>first<!>, <!UNRESOLVED_REFERENCE!>second<!>,) in x) {}
    for ((<!UNRESOLVED_REFERENCE!>first<!>) in x) {}
    for ((<!UNRESOLVED_REFERENCE!>first<!>: String) in x) {}
    for ((a: <!INITIALIZER_TYPE_MISMATCH!>Int<!>, b: <!INITIALIZER_TYPE_MISMATCH!>String<!>) in x) {}
    for ((<!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>) in x) {}
    for ((<!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>: String) in x) {}

    // renaming
    for ((aa = <!UNRESOLVED_REFERENCE!>first<!>) in x) {}
    for ((aa: String = <!UNRESOLVED_REFERENCE!>first<!>) in x) {}
    for ((first: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> a, second: String <!INITIALIZER_TYPE_MISMATCH!>=<!> b) in x) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}

    foo { (<!UNRESOLVED_REFERENCE!>first<!>, <!UNRESOLVED_REFERENCE!>second<!>,) -> }
    foo { (<!UNRESOLVED_REFERENCE!>first<!>) -> }
    foo { (<!UNRESOLVED_REFERENCE!>first<!>: String) -> }
    foo { (a: <!INITIALIZER_TYPE_MISMATCH!>Int<!>, b: <!INITIALIZER_TYPE_MISMATCH!>String<!>) -> }
    foo { (<!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>) -> }
    foo { (<!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>: String) -> }

    // renaming
    foo { (aa = <!UNRESOLVED_REFERENCE!>first<!>) -> }
    foo { (aa: String = <!UNRESOLVED_REFERENCE!>first<!>) -> }
    foo { (first: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> a, second: String <!INITIALIZER_TYPE_MISMATCH!>=<!> b) -> }
}

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, forLoop, functionDeclaration, functionalType,
ifExpression, lambdaLiteral, localFunction, localProperty, primaryConstructor, propertyDeclaration */
