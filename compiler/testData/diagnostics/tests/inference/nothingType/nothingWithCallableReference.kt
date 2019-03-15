// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

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

    select3(null, f1, <!TYPE_MISMATCH!>::foo<!>)
    select3(null, f2, <!TYPE_MISMATCH!>::foo<!>)

    dependantSelect2(null, ::foo)
    dependantSelect3(null, ::foo, ::cloneFoo)
    dependantSelect3(null, f1, ::bar)
    dependantSelect3(null, ::bar, f1)

    // These errors are actually can be fixed (and, probably, should) if we force resolution of callable reference
    dependantSelect3(null, ::foo, <!TYPE_MISMATCH!>::bar<!>)
    dependantSelect3(null, ::bar, <!TYPE_MISMATCH!>::foo<!>)
    dependantSelect3(null, f1, <!TYPE_MISMATCH!>::foo<!>)
    dependantSelect3(null, <!TYPE_MISMATCH!>::foo<!>, f1)
}
