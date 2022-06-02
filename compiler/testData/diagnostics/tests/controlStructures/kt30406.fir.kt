// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !CHECK_TYPE
// Issue: KT-30406

interface Option<out T> {
    val s: String
}
class Some<T>(override val s: String) : Option<T>
class None(override val s: String = "None") : Option<Int>

fun test(a: Int): Option<Any> =
    if (a == 239)
        Some("239")
    else
        None()