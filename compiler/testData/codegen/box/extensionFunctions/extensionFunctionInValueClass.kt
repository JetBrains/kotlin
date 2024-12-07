// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS

OPTIONAL_JVM_INLINE_ANNOTATION
value class ValueClass(private val s: Int.()-> String) {
    fun print(): String {
        return s(1)
    }
}

val Int.a : String
    get() = "O"

fun foo(a: Int): String = "K"

fun box(): String {
    return ValueClass(Int::a).print() + ValueClass(::foo).print()
}