open class Base
class Derived: Base()
operator fun Derived.inc(): Derived { return Derived() }

public fun box() : String {
    var i : Base
    i = Derived()
    val j = i++

    return if (j is Derived && i is Derived) "OK" else "fail j = $j i = $i"
}
