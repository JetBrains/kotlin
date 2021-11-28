// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

fun baz(i: Int) = i
fun <T> bar(x: T): T = TODO()

fun nullableFun(): ((Int) -> Int)? = null

fun test() {
    val x1: (Int) -> Int = bar(if (true) ::baz else ::baz)
    val x2: (Int) -> Int = bar(nullableFun() ?: ::baz)
    val x3: (Int) -> Int = bar(::baz <!USELESS_ELVIS!>?: ::baz<!>)

    val i = 0
    val x4: (Int) -> Int = bar(when (i) {
                                   10 -> ::baz
                                   20 -> ::baz
                                   else -> ::baz
                               })

    val x5: (Int) -> Int = bar(::baz<!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>)

    (if (true) ::baz else ::baz)(1)
}
