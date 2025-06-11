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
}

fun <T> accept(t: T) {}
