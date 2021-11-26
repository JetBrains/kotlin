// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Value(val value: Any)

object Foo {
    fun foo(value: Value) {
        res = value.value as String
    }

    fun bar(value: Value?) {
        res = value?.value as String?
    }
}

var res: String? = "FAIL"

fun box(): String {
    Value("OK").let(Foo::foo)
    if (res != "OK") return "FAIL 1: $res"
    res = "FAIL 2"

    Value("OK").let(Foo::bar)
    if (res != "OK") return "FAIL 3: $res"
    res = "FAIL 4"

    null.let(Foo::bar)
    if (res != null) return "FAIL 5: $res"

    return "OK"
}