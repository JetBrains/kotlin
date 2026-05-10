// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

class A

class ResolutionA
class ResolutionB

context(a: A)
fun foo0(): ResolutionA {
    return ResolutionA()
}

context(a: A)
val foo0: () -> ResolutionB
    get() = { ResolutionB() }

context(a: A)
<!CONTEXTUAL_OVERLOAD_SHADOWED!>val foo1: () -> ResolutionA<!>
    get() = { ResolutionA() }

val foo1: context(A) () -> ResolutionB
    get() = { ResolutionB() }

fun foo2(): ResolutionA {
    return ResolutionA()
}

context(a: A)
val foo2: () -> ResolutionB
    get() = { ResolutionB() }

context(a: A)
fun foo3(): ResolutionA {
    return ResolutionA()
}

val foo3: () -> ResolutionB
    get() = { ResolutionB() }

context(a: A)
fun usage() {
    val t0 = foo0()
    val t1 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo1<!>()
    val t2 = foo2()
    val t3 = foo3()
}

fun usage2() {
    fun <A, R> context(context: A, block: context(A) () -> R): R = block(context)

    val t0 = <!NO_CONTEXT_ARGUMENT!>foo0<!>()
    val t1 = <!NO_CONTEXT_ARGUMENT!>foo1<!>()
    val t1_ = foo1.let { context(A()) { it() } }
    val t2 = foo2()
    val t3 = foo3()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, functionalType, getter, lambdaLiteral,
propertyDeclaration, propertyDeclarationWithContext, stringLiteral, typeWithContext */
