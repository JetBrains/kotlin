// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

class Controller<T> {
    suspend fun yield(t: T) {}
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

val <!NI;IMPLICIT_NOTHING_PROPERTY_TYPE!>test1<!> = <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    apply {
        yield(<!NI;CONSTANT_EXPECTED_TYPE_MISMATCH!>4<!>)
    }
}

val <!NI;IMPLICIT_NOTHING_PROPERTY_TYPE!>test2<!> = <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    yield(<!NI;TYPE_MISMATCH!>B<!>)
    apply {
        yield(<!NI;TYPE_MISMATCH!>C<!>)
    }
}

val <!NI;IMPLICIT_NOTHING_PROPERTY_TYPE!>test3<!> = <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    this.<!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>let<!> {
        yield(<!NI;TYPE_MISMATCH!>B<!>)
    }

    apply {
        yield(<!NI;TYPE_MISMATCH!>C<!>)
    }
}

interface A
object B : A
object C : A