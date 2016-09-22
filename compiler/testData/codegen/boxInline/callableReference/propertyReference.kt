// FILE: 1.kt

package test

class Foo(val a: String)

inline fun <T> test(receiver: T, selector: (T) -> String): String {
    return selector(receiver)
}

// FILE: 2.kt

import test.*

fun box(): String {
    return test(Foo("OK"), Foo::a)
}