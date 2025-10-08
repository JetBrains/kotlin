// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring

class Tuple(val a: String, val b: Int)

fun declaration(x: Tuple) {
    if (true) { (val a, var <!UNRESOLVED_REFERENCE!>second<!>,) = x }
    if (true) { (var <!UNRESOLVED_REFERENCE!>first<!>) = x }
    if (true) { (val <!UNRESOLVED_REFERENCE!>first<!>: String) = x }
    if (true) { (val aa = <!UNRESOLVED_REFERENCE!>first<!>) = x }
    if (true) { (val aa: String = <!UNRESOLVED_REFERENCE!>first<!>) = x }
    if (true) { <!INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION!>(val <!UNRESOLVED_REFERENCE!>first<!>)<!> }
    if (true) { <!INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION!>(val aa: String = <!UNRESOLVED_REFERENCE!>first<!>)<!> }
    if (true) { (val a: <!INITIALIZER_TYPE_MISMATCH!>Int<!>, val b: <!INITIALIZER_TYPE_MISMATCH!>String<!>) = x }
    if (true) { (val first: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> a, val second: String <!INITIALIZER_TYPE_MISMATCH!>=<!> b) = x }
    if (true) { (val <!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>) = x }
    if (true) { (val <!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>: String) = x }
}

fun loop(x: List<Tuple>) {
    for ((val <!UNRESOLVED_REFERENCE!>first<!>, val <!UNRESOLVED_REFERENCE!>second<!>,) in x) {}
    for ((val <!UNRESOLVED_REFERENCE!>first<!>) in x) {}
    for ((val <!UNRESOLVED_REFERENCE!>first<!>: String) in x) {}
    for ((val aa = <!UNRESOLVED_REFERENCE!>first<!>) in x) {}
    for ((val aa: String = <!UNRESOLVED_REFERENCE!>first<!>) in x) {}
    for ((val a: <!INITIALIZER_TYPE_MISMATCH!>Int<!>, val b: <!INITIALIZER_TYPE_MISMATCH!>String<!>) in x) {}
    for ((val first: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> a, val second: String <!INITIALIZER_TYPE_MISMATCH!>=<!> b) in x) {}
    for ((val <!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>) in x) {}
    for ((val <!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>: String) in x) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}

    foo { (val <!UNRESOLVED_REFERENCE!>first<!>, val <!UNRESOLVED_REFERENCE!>second<!>,) -> }
    foo { (val <!UNRESOLVED_REFERENCE!>first<!>) -> }
    foo { (val <!UNRESOLVED_REFERENCE!>first<!>: String) -> }
    foo { (val aa = <!UNRESOLVED_REFERENCE!>first<!>) -> }
    foo { (val aa: String = <!UNRESOLVED_REFERENCE!>first<!>) -> }
    foo { (val a: <!INITIALIZER_TYPE_MISMATCH!>Int<!>, val b: <!INITIALIZER_TYPE_MISMATCH!>String<!>) -> }
    foo { (val first: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> a, val second: String <!INITIALIZER_TYPE_MISMATCH!>=<!> b) -> }
    foo { (val <!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>) -> }
    foo { (val <!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>: String) -> }
}

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, forLoop, functionDeclaration, functionalType,
ifExpression, lambdaLiteral, localFunction, localProperty, primaryConstructor, propertyDeclaration */
