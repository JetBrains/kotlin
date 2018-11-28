// RUNTIME_WITH_FULL_JDK

import java.util.*

fun foo(f: () -> ArrayDeque<*>) {}

fun test() {
    foo <caret>{ ArrayDeque<Int>() }
}
