// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// NI_EXPECTED_FILE

// ISSUE: KT-32383

class Inv<T>
class Out<out T>
class In<in T>

fun <T> invOf(vararg t: T): Inv<T> = null!!
fun <T> outOf(vararg t: T): Out<T> = null!!
fun <T> inOf(vararg t: T): In<T> = null!!

interface Foo
abstract class Bar<out TFoo : Foo>

enum class AFoo : Foo
object A : Bar<AFoo>()

enum class BFoo : Foo
object B : Bar<BFoo>()

val invs = invOf(A, B)
val outs = outOf(A, B)
val ins = inOf(A, B)
