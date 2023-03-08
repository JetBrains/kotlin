// WITH_REFLECT

import kotlin.reflect.KFunction0

fun foo(arg: KFunction0<Unit>) {}

fun main() {
    foo(<!ARGUMENT_TYPE_MISMATCH!>fun() {}<!>) // K1: TYPE_MISMATCH, K2: ok in compile-time & CCE at run-time
}
