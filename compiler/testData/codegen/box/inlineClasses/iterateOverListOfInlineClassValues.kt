// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Foo(val arg: String)

fun box(): String {
    val ls = listOf(Foo("abc"), Foo("def"))
    var res = ""
    for (el in ls) {
        res += el.arg
    }

    return if (res != "abcdef") "Fail" else "OK"
}
