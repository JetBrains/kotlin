// TARGET_BACKEND: JVM
// WITH_STDLIB
@JvmInline
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