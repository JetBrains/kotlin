// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring
data class Tuple(val first: String, val second: Int)

fun declaration(x: Tuple) {
    if (true) { val [first, second,] = x }
    if (true) { var [first] = x }
    if (true) { val [first: String] = x }
}

fun loop(x: List<Tuple>) {
    for ([first, second] in x) {}
    for ([first] in x) {}
    for ([first: String] in x) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}

    foo { [first, second,] -> }
    foo { [first] -> }
    foo { [first: String] -> }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, destructuringDeclaration, forLoop, functionDeclaration, functionalType,
ifExpression, lambdaLiteral, localFunction, localProperty, primaryConstructor, propertyDeclaration */
