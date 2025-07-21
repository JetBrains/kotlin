// RUN_PIPELINE_TILL: FRONTEND

class Tuple(val a: String, val b: Int)

fun declaration(x: Tuple) {
    if (true) { (val a, <!UNRESOLVED_REFERENCE!>var second<!>,) = x }
    if (true) { (<!UNRESOLVED_REFERENCE!>var first<!>) = x }
    if (true) { (<!UNRESOLVED_REFERENCE!>val first: String<!>) = x }
    if (true) { (val aa = <!UNRESOLVED_REFERENCE!>first<!>) = x }
    if (true) { (val aa: String = <!UNRESOLVED_REFERENCE!>first<!>) = x }
    if (true) { <!INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION!>(<!UNRESOLVED_REFERENCE!>val first<!>)<!> }
    if (true) { <!INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION!>(val aa: String = <!UNRESOLVED_REFERENCE!>first<!>)<!> }
    if (true) { (val a: <!INITIALIZER_TYPE_MISMATCH!>Int<!>, val b: <!INITIALIZER_TYPE_MISMATCH!>String<!>) = x }
    if (true) { (val first: Int = <!INITIALIZER_TYPE_MISMATCH!>a<!>, val second: String = <!INITIALIZER_TYPE_MISMATCH!>b<!>) = x }
}

fun loop(x: List<Tuple>) {
    for ((<!UNRESOLVED_REFERENCE!>val first<!>, <!UNRESOLVED_REFERENCE!>val second<!>,) in x) {}
    for ((<!UNRESOLVED_REFERENCE!>val first<!>) in x) {}
    for ((<!UNRESOLVED_REFERENCE!>val first: String<!>) in x) {}
    for ((val aa = <!UNRESOLVED_REFERENCE!>first<!>) in x) {}
    for ((val aa: String = <!UNRESOLVED_REFERENCE!>first<!>) in x) {}
    for ((val a: <!INITIALIZER_TYPE_MISMATCH!>Int<!>, val b: <!INITIALIZER_TYPE_MISMATCH!>String<!>) in x) {}
    for ((val first: Int = <!INITIALIZER_TYPE_MISMATCH!>a<!>, val second: String = <!INITIALIZER_TYPE_MISMATCH!>b<!>) in x) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}

    foo { (<!UNRESOLVED_REFERENCE!>val first<!>, <!UNRESOLVED_REFERENCE!>val second<!>,) -> }
    foo { (<!UNRESOLVED_REFERENCE!>val first<!>) -> }
    foo { (<!UNRESOLVED_REFERENCE!>val first: String<!>) -> }
    foo { (val aa = <!UNRESOLVED_REFERENCE!>first<!>) -> }
    foo { (val aa: String = <!UNRESOLVED_REFERENCE!>first<!>) -> }
    foo { (val a: <!INITIALIZER_TYPE_MISMATCH!>Int<!>, val b: <!INITIALIZER_TYPE_MISMATCH!>String<!>) -> }
    foo { (val first: Int = <!INITIALIZER_TYPE_MISMATCH!>a<!>, val second: String = <!INITIALIZER_TYPE_MISMATCH!>b<!>) -> }
}

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, forLoop, functionDeclaration, functionalType,
ifExpression, lambdaLiteral, localFunction, localProperty, primaryConstructor, propertyDeclaration */
