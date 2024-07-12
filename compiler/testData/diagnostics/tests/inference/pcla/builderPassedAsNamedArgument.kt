// FIR_IDENTICAL
class Controller<T>

fun <S> generate1(g: suspend (Controller<S>) -> Unit): S = TODO()
fun <S> generate2(g: suspend Controller<S>.() -> Unit): S = TODO()

fun foo(c: Controller<String>) {}

fun foo() {
    val t1 = generate1 {
        foo(c = it)
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>t1<!>

    val t2 = generate2 {
        foo(c = this)
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>t2<!>
}
