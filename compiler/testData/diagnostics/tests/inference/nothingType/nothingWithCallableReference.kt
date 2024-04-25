// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun <K> select2(x: K, y: K): K = TODO()
fun <K> select3(x: K, y: K, z: K): K = TODO()

fun <K : S, S> dependantSelect2(x: K, y: S) {}
fun <K : S, S> dependantSelect3(x: K, y: K, z: S) {}

fun foo() {}
fun cloneFoo() {}
fun bar(x: Int) {}

fun test(f1: (Int) -> Unit, f2: kotlin.Function1<Int, Unit>) {
    select2(null, ::foo)
    select3(null, f1, ::bar)
    select3(null, f2, ::bar)

    select3(null, f1, ::foo)
    select3(null, f2, ::foo)

    dependantSelect2(null, ::foo)
    dependantSelect3(null, ::foo, ::cloneFoo)
    dependantSelect3(null, f1, ::bar)
    dependantSelect3(null, ::bar, f1)

    dependantSelect3(null, ::foo, ::bar)
    dependantSelect3(null, ::bar, ::foo)
    dependantSelect3(null, f1, ::foo)
    dependantSelect3(null, ::foo, f1)
}
