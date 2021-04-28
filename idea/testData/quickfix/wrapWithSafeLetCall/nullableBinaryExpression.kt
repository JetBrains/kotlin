// "Wrap with '?.let { ... }' call" "true"
// WITH_RUNTIME

interface A

operator fun A?.plus(a: A?): A? = this

fun test(a1: A, a2: A) {
    notNull(<caret>a1 + a2)
}

fun notNull(t: A): A = t
