// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

interface Receiver
interface Parameter
typealias LambdaWithReceiver = Receiver.(Parameter) -> Unit

fun Receiver.method(param: Parameter): LambdaWithReceiver = TODO()

enum class E { VALUE }

fun <K> id(x: K): K = x

class SomeClass {
    val e = E.VALUE

    val withoutType: LambdaWithReceiver
        get() = when (e) {
            E.VALUE -> { param ->
                <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>method<!>(param)
            }
        }

    val withExplicitType: LambdaWithReceiver
        get() = when (e) {
            E.VALUE -> { param: Parameter ->
                <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>method<!>(param)
            }
        }
}

class OtherClass {
    val ok: LambdaWithReceiver
        get() = { param: Parameter ->
            method(param)
        }
}

val e2 = E.VALUE
val staticWithExplicitType: LambdaWithReceiver
    get() = when (e2) {
        E.VALUE -> { param: Parameter ->
            <!UNRESOLVED_REFERENCE!>method<!>(param)
        }
    }
