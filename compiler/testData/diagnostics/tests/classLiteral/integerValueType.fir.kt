// KT-13110 Strange type mismatch error on class literal with integer receiver expression

import kotlin.reflect.KClass

fun f(x: KClass<out Int>) {}

fun test() {
    f(42::class)
    f((40 + 2)::class)
    <!UNRESOLVED_REFERENCE!>42::toInt<!>
}
