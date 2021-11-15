// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val x: Int)

class A {
    fun foo() = Z(42)
}

fun test(a: A?) = a?.foo()!!

fun box(): String {
    val t = test(A())
    if (t.x != 42) throw AssertionError("$t")
    return "OK"
}