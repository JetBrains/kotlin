// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +NameBasedDestructuring, +ContextParameters

class Tuple(val first: String, val second: Int)

context(instance: Tuple)
fun ctx() = instance

context(instance: Tuple)
val ctxProp: Tuple get() = instance

context(list: List<Tuple>)
fun ctxList() = list

fun declaration(instance1: Tuple, instance2: Tuple) {
    (val first: String, val second: Int) = <!NO_CONTEXT_ARGUMENT!>ctx<!>()
    (val _: String = first) = <!NO_CONTEXT_ARGUMENT!>ctx<!>()

    context(instance1, instance2) {
        (val first: String, val second: Int) = <!AMBIGUOUS_CONTEXT_ARGUMENT!>ctx<!>()
        (val _: String = first) = <!AMBIGUOUS_CONTEXT_ARGUMENT!>ctx<!>()
    }
}

fun loop(instance1: Tuple, instance2: Tuple) {
    for ((val first: String, val second: Int) in listOf(<!NO_CONTEXT_ARGUMENT!>ctx<!>())) { }
    for ((val _: String = first) in listOf(<!NO_CONTEXT_ARGUMENT!>ctx<!>())) { }

    context(instance1, instance2) {
        for ((val first: String, val second: Int) in listOf(<!AMBIGUOUS_CONTEXT_ARGUMENT!>ctx<!>())) { }
        for ((val _: String = first) in listOf(<!AMBIGUOUS_CONTEXT_ARGUMENT!>ctx<!>())) { }
    }
}

fun lambda(instance1: Tuple, instance2: Tuple) {
    <!NO_CONTEXT_ARGUMENT!>ctx<!>().let { (val first: String, val second: Int) -> }
    <!NO_CONTEXT_ARGUMENT!>ctx<!>().let { (val _: String = first) -> }

    context(instance1, instance2) {
        <!AMBIGUOUS_CONTEXT_ARGUMENT!>ctx<!>().let { (val first: String, val second: Int) -> }
        <!AMBIGUOUS_CONTEXT_ARGUMENT!>ctx<!>().let { (val _: String = first) -> }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, forLoop, functionDeclaration,
functionDeclarationWithContext, functionalType, ifExpression, integerLiteral, lambdaLiteral, localFunction,
localProperty, primaryConstructor, propertyDeclaration, stringLiteral, unnamedLocalVariable */
