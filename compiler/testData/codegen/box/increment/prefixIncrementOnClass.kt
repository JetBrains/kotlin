// IGNORE_BACKEND_FIR: JVM_IR
interface Base
class Derived: Base
class Another: Base
operator fun Base.inc(): Derived { return Derived() }

public fun box() : String {
    var i : Base
    i = Another()
    val j = ++i

    return if (j is Derived && i is Derived) "OK" else "fail j = $j i = $i"
}
