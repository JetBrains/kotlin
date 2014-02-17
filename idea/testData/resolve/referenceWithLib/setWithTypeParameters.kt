package test

import dependency.set

fun foo(a: List<Int>) {
    a[<caret>"bar"] = 3
}

// REF: (for jet.List<T> in dependency).set(jet.String,T)
