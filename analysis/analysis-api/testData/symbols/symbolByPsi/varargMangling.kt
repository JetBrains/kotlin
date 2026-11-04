// WITH_STDLIB
@JvmInline
value class Wrapper(val s: String)

fun nullableUnsigned(vararg a: UInt?)
fun unsigned(vararg b: UInt)
fun <T : Wrapper> boundedByValueClass(vararg c: T)
