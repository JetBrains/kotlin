// WITH_REFLECT

import kotlin.reflect.KFunction1

fun <T, R> foo(p: T, mapper : (T) -> R): R {
    mapper(p)
}

fun bar() {
    foo(1, x<caret>y as KFunction1<Int, String>)
}
