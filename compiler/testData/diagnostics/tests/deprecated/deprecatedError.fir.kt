// !DIAGNOSTICS: -UNUSED_PARAMETER
@Deprecated("alas", level = DeprecationLevel.ERROR)
fun foo() {}

@Deprecated("alas", level = DeprecationLevel.ERROR)
class C

fun test(c: C) {
    foo()
    C()
}