// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring
data class Tuple(val first: String, val second: Int)
class NonDataTuple(val first: String, val second: Int)

fun declaration(x: Tuple, y: NonDataTuple) {
    if (true) { [val first, var second: String,] = <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>x<!> }
    if (true) { [var first: Int] = <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>x<!> }
    if (true) { [val first] = <!COMPONENT_FUNCTION_MISSING!>y<!> }
    if (true) { <!INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION!>[val first]<!> }
}

fun loop(x: List<Tuple>, y: NonDataTuple) {
    for ([val first, val second: String,] in <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>x<!>) {}
    for ([val first: Int] in <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>x<!>) {}
    for ([val first: String] in <!COMPONENT_FUNCTION_MISSING, ITERATOR_MISSING!>y<!>) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}
    fun bar(f: (NonDataTuple) -> Unit) {}

    foo { [val first, <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>val second: String<!>,] -> }
    foo { [<!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>val first: Int<!>] -> }
    bar { <!COMPONENT_FUNCTION_MISSING!>[val first]<!> -> }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, destructuringDeclaration, forLoop, functionDeclaration, functionalType,
ifExpression, lambdaLiteral, localFunction, localProperty, primaryConstructor, propertyDeclaration */
