// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ParseLambdaWithSuspendModifier, +DiscriminateSuspendInOverloadResolution

fun foo0(f: () -> Unit): String = ""
fun foo0(f: suspend () -> Unit): Int = 0

fun foo0Receiver(f: String.() -> Unit): String = ""
fun foo0Receiver(f: suspend String.() -> Unit): Int = 0

fun foo1(f: (String) -> Unit): String = ""
fun foo1(f: suspend (String) -> Unit): Int = 0

fun foo1Receiver(f: String.(String) -> Unit): String = ""
fun foo1Receiver(f: suspend String.(String) -> Unit): Int = 0

fun fooDifferentArity(f: () -> Unit): String = ""
fun fooDifferentArity(f: suspend (String) -> Unit): Int = 0

fun fooDifferentArityX(f: (String) -> Unit): String = ""
fun fooDifferentArityX(f: suspend () -> Unit): Int = 0

fun test() {
    accept<String>(foo0({}))
    accept<Int>(foo0(suspend {}))

    accept<String>(foo0Receiver({}))
    accept<Int>(foo0Receiver(suspend {}))

    accept<String>(foo1({ it }))
    accept<Int>(foo1(suspend { it }))

    accept<String>(foo1({ x -> }))
    accept<Int>(foo1(suspend { x -> }))

    accept<String>(foo1Receiver({}))
    accept<Int>(foo1Receiver(suspend {}))

    accept<String>(fooDifferentArity({}))
    accept<Int>(fooDifferentArity(suspend {}))

    accept<String>(fooDifferentArityX({}))
    accept<Int>(fooDifferentArityX(suspend {}))
}

fun <T> accept(t: T) {}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, nullableType, stringLiteral,
suspend, typeParameter, typeWithExtension */
