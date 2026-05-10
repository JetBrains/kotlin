// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -EnableNameBasedDestructuringShortForm
// RENDER_DIAGNOSTICS_FULL_TEXT
data class Tuple(val a: String, val b: Int)

fun declaration(x: Tuple) {
    if (true) { val (a <!UNSUPPORTED_FEATURE!>=<!> first) = x }
    if (true) { val (a, b <!UNSUPPORTED_FEATURE!>=<!> second) = x }
    if (true) { val (a: String <!UNSUPPORTED_FEATURE!>=<!> first) = x }
}

fun loop(x: List<Tuple>) {
    for ((a <!UNSUPPORTED_FEATURE!>=<!> first) in x) {}
    for ((a, b <!UNSUPPORTED_FEATURE!>=<!> second) in x) {}
    for ((a: String <!UNSUPPORTED_FEATURE!>=<!> first) in x) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}

    foo { (a <!UNSUPPORTED_FEATURE!>=<!> first) -> }
    foo { (a, b <!UNSUPPORTED_FEATURE!>=<!> second) -> }
    foo { (a: String <!UNSUPPORTED_FEATURE!>=<!> first) -> }
}

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, forLoop, functionDeclaration, functionalType,
ifExpression, lambdaLiteral, localFunction, localProperty, primaryConstructor, propertyDeclaration */
