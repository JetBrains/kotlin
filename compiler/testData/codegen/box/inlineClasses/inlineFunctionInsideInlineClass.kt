// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Foo(val x: Int) {
    inline fun inc(): Foo = Foo(x + 1)
}

fun box(): String {
    val a = Foo(0)
    val b = a.inc().inc()

    if (b.x != 2) return "fail"

    return "OK"
}