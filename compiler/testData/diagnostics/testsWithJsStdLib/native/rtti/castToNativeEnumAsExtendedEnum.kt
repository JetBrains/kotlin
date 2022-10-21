// !LANGUAGE: +SafeExternalEnums
external enum class Foo : Enum<Foo> { A, B }

fun box(a: Any, b: Any): Pair<Foo, Foo?> {
    return Pair(a as Foo, b as? Foo)
}