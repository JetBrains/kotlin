// DIAGNOSTICS: -NOTHING_TO_INLINE -UNUSED_PARAMETER

interface SomeInterface

fun someFun(i: SomeInterface) {}

private fun foo1() = object {}

private inline fun foo2() = object {}

private inline fun foo3() {
    someFun(object : SomeInterface {})
}

private inline fun foo4() { foo2() }

internal inline fun bar() {
    foo1()
    foo2()
    foo3()
    foo4()
}
