// WITH_STDLIB
package main

import kotlin.collections.MutableList

abstract class B : MutableList<String>() {}

fun usage(b : B?) {
    if (b != null) {
        <expr>b.clear()</expr>
    }
}
