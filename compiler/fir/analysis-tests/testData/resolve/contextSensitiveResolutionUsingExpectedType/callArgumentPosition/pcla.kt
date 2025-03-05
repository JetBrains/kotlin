// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    X, Y
}

class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun foo(m: MyEnum) {
    generate {
        yield(m)
        yield(X)
    }

    generate {
        yield(X)
        yield(m)
    }

    val x: MyEnum = generate {
        yield(X)
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>generate<!> {
        yield(<!UNRESOLVED_REFERENCE!>X<!>)
    }
}
