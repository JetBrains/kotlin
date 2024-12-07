// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-61747

interface A {
    operator fun getValue(x: Any?, y: Any?): String = ""
}

fun <T> foo(x: (T) -> Unit): T = TODO()

fun bar1(a: A) {
    val x <!DELEGATE_SPECIAL_FUNCTION_MISSING!>by<!> foo { x: A -> }
}

fun bar2(a: A) {
    operator fun <F> F.provideDelegate(x: Any?, y: Any?): F {
        return this
    }

    val x <!DELEGATE_SPECIAL_FUNCTION_MISSING!>by<!> foo { x: A -> }
}