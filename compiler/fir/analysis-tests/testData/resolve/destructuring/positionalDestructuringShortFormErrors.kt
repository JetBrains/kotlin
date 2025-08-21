// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring
data class Tuple(val first: String, val second: Int)
class NonDataTuple(val first: String, val second: Int)

fun declaration(x: Tuple, y: NonDataTuple) {
    if (true) { val [first, second: String,] = <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>x<!> }
    if (true) { var [first: Int] = <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>x<!> }
    if (true) { val [first] = <!COMPONENT_FUNCTION_MISSING!>y<!> }
    if (true) { <!INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION!>val [first]<!> }
}

fun loop(x: List<Tuple>, y: NonDataTuple) {
    for ([first, second: String,] in <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>x<!>) {}
    for ([first: Int] in <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>x<!>) {}
    for ([first: String] in <!COMPONENT_FUNCTION_MISSING, ITERATOR_MISSING!>y<!>) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}
    fun bar(f: (NonDataTuple) -> Unit) {}

    foo { [first, <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>second: String<!>,] -> }
    foo { [<!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>first: Int<!>] -> }
    bar { <!COMPONENT_FUNCTION_MISSING!>[first]<!> -> }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, destructuringDeclaration, forLoop, functionDeclaration, functionalType,
ifExpression, lambdaLiteral, localFunction, localProperty, primaryConstructor, propertyDeclaration */
