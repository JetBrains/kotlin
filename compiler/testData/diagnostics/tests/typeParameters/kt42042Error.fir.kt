// !LANGUAGE: +ProperTypeInferenceConstraintsProcessing

sealed class Subtype<A1, B1> {
    abstract fun cast(value: A1): B1
    class Trivial<A2 : B2, B2> : Subtype<A2, B2>() {
        override fun cast(value: A2): B2 = value
    }
}

fun <A, B> unsafeCast(value: A): B {
    val proof: Subtype<A, B> = Subtype.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Trivial<!>()
    return proof.cast(value)
}
