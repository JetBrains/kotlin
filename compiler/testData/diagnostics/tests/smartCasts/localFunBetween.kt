open class Base
class Derived: Base()

fun bar(derived: Derived) = derived

fun trans(n: Int, f: (Int) -> Boolean) = if (f(n)) n else null

fun foo() {
    val base: Base = Derived()
    if (base is Derived) {
        fun can(n: Int) = n > 0
        trans(42, ::can)
        bar(<!DEBUG_INFO_SMARTCAST!>base<!>)
    }
}
