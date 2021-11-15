// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Foo(val s: Any) {
    fun isString(): Boolean = s is String
}

class Box<T>(val x: T)

fun box(): String {
    val f = Foo("string")
    val g = Box(f)
    val r = g.x.isString()

    if (!r) return "Fail"

    return "OK"
}