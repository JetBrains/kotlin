// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Foo(val x: Int) : Comparable<Foo> {
    override fun compareTo(other: Foo): Int {
        return 10
    }
}

fun box(): String {
    val f1 = Foo(42)
    val ff1: Comparable<Foo> = f1

    if (ff1.compareTo(f1) != 10) return "Fail"

    return "OK"
}