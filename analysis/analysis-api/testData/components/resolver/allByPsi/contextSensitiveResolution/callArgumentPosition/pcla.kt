// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// IGNORE_FIR
// ^KT-79993

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

    generate {
        yield(X)
    }
}
