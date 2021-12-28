// WITH_STDLIB
// WITH_REFLECT
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Foo<T: String>(val x: T) {
    fun bar(f: Foo<T>, i: Int): Foo<String> = Foo(x + f.x + i)
}

fun box(): String {
    val f = Foo("original")
    val function1 = f::bar
    val result1 = function1.invoke(Foo("+argument+"), 42)
    if (result1.x != "original+argument+42") return "Fail first"

    val result2 = Foo<String>::bar.invoke(Foo("explicit"), Foo("+argument2+"), 10)
    if (result2.x != "explicit+argument2+10") return "Fail second"

    return "OK"
}
