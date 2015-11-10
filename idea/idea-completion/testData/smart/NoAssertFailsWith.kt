import kotlin.test.*

interface I<T>

fun <T> foo(i: I<T>){}

fun bar() {
    foo(ass<caret>)
}

// ABSENT: assertFailsWith
