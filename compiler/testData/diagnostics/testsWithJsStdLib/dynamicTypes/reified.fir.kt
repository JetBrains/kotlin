// !DIAGNOSTICS: -UNUSED_PARAMETER -REIFIED_TYPE_PARAMETER_NO_INLINE

fun <reified T> foo(t: T) {}
class C<reified T>(t: T)

fun test(d: dynamic) {
    foo<dynamic>(d)
    foo(d)

    C<dynamic>(d)
    C(d)
}
