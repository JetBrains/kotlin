package test

import dependency.set

fun foo(a: List<Int>) {
    a[<caret>"bar"] = 3
}

// REF: (for kotlin.List<T> in dependency).set(kotlin.String,T)
