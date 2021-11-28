// WITH_STDLIB
// WITH_REFLECT

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Foo(val x: String) {
    fun bar(f: Foo, i: Int): Foo = Foo(x + f.x + i)
}

fun box(): String {
    val f = Foo("original")
    val function1 = f::bar
    val result1 = function1.invoke(Foo("+argument+"), 42)
    if (result1.x != "original+argument+42") return "Fail first"

    val result2 = Foo::bar.invoke(Foo("explicit"), Foo("+argument2+"), 10)
    if (result2.x != "explicit+argument2+10") return "Fail second"

    return "OK"
}
